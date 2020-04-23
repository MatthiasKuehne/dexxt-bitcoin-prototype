package DeXTT;

import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.DataStructure.ProofOfIntentData;
import DeXTT.DataStructure.ProofOfIntentFull;
import DeXTT.Exception.FullClaimMissingException;
import DeXTT.Exception.UnconfirmedTransactionExecutionException;
import DeXTT.Transaction.*;
import Events.*;
import com.google.common.eventbus.EventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static Configuration.Constants.MINTING_ADDRESS;
import static Configuration.Constants.WITNESS_REWARD;

// more or less "PBT.sol", should do the same
public class Wallet {

    private static final Logger logger = LogManager.getLogger();

    private GlobalEventBus globalEventBus;
    private EventBus localEventBus;

    private String urlRPC;
    private String chainInfo;

    private BigInteger totalSupply = BigInteger.valueOf(0);

    private Map<DeXTTAddress, BigInteger> balances; // maps address to balance

    private Map<DeXTTAddress, BigInteger> senderLock; // maps address to PoI (keccak256 hash, alphaData) that currently locks it
    private Map<BigInteger, ProofOfIntentFull> ongoingPois; // maps a PoI hash (alphaData) to the actual PoI data
    private Map<BigInteger, ProofOfIntentFull> ongoingVetoPoi; // maps a PoI hash (alphaData) to the actual PoI data, saves all PoIs that are in conflict with others

    private Map<BigInteger, DeXTTAddress> contestWinner; // maps PoI hash to current winner of its contest
    private Map<BigInteger, BigInteger> contestSignature; // maps PoI hash to its current best contestSignature (of current winner)

    // key for all: "from-address", the sender address of the PoIs that started a Veto
    private Map<DeXTTAddress, Boolean> senderInvalid; // maps "from-address" to boolean, indicating if the address is invalid as a sender
    private Map<DeXTTAddress, Date> vetoT1; // maps "form-address" to the endtime (t1) of corersponding vetoContest
    private Map<DeXTTAddress, DeXTTAddress> vetoWinner; // maps "from-address" to current winner of its veto-contest
    private Map<DeXTTAddress, BigInteger> vetoSignature; // maps "from-address" to its current best vetoContestSignature (of current winner)
    private Map<DeXTTAddress, Boolean> vetoFinalized; // maps "from-address" to boolean, indicating if corresponding vetoContest has already been finalized

    public Wallet(EventBus localEventBus, String urlRPC) {
        this.globalEventBus = GlobalEventBus.getInstance();
        this.localEventBus = localEventBus;
        this.urlRPC = urlRPC;
        this.chainInfo = "[" + urlRPC + "]";
        this.balances = new HashMap<>();
        this.senderLock = new HashMap<>();
        this.ongoingPois = new HashMap<>();
        this.ongoingVetoPoi = new HashMap<>();
        this.contestWinner = new HashMap<>();
        this.contestSignature = new HashMap<>();
        this.senderInvalid = new HashMap<>();
        this.vetoT1 = new HashMap<>();
        this.vetoWinner = new HashMap<>();
        this.vetoSignature = new HashMap<>();
        this.vetoFinalized = new HashMap<>();
    }

    public BigInteger totalSupply() {
        return this.totalSupply;
    }

    public BigInteger balanceOf(DeXTTAddress address) {
        return this.balances.getOrDefault(address, BigInteger.valueOf(0));
    }

    public BigInteger lockStatus(DeXTTAddress address) {
        return this.senderLock.get(address);
    }

    public Date vetoStatus(DeXTTAddress address) {
        return this.vetoT1.get(address);
    }

    public boolean invalidStatus(DeXTTAddress address) {
        return this.senderInvalid.getOrDefault(address, false);
    }

    public DeXTTAddress vetoWinner(DeXTTAddress conflictingPoiSender) {
        return this.vetoWinner.get(conflictingPoiSender);
    }

    public boolean isOngoingPoi(BigInteger poiHash) {
        return this.ongoingPois.containsKey(poiHash);
    }

    /**
     *
     * @param poiHash
     * @return  null if no such poiHash
     */
    public DeXTTAddress getContestWinner(BigInteger poiHash) {
        return this.contestWinner.get(poiHash);
    }

    public DeXTTAddress getVetoContestWinner(DeXTTAddress conflictingPoiSender) {
        return this.vetoWinner.get(conflictingPoiSender);
    }

    /**
     * unlocks sender, only for testing/prototype, not for later use!!!
     * @param sender
     */
    public void unlock(DeXTTAddress sender) {
        this.senderLock.remove(sender);
    }

    /**
     * makes sender valid again, only for testing/prototype, not for later use!!!
     * @param sender
     */
    public void makeSenderValid(DeXTTAddress sender) {
        if (this.vetoFinalized.get(sender)) {
            this.senderInvalid.remove(sender);
            this.vetoT1.remove(sender);
//            this.vetoWinner.remove(sender); // not needed to reset, needed for consistency checks
            this.vetoSignature.remove(sender);
            this.vetoFinalized.remove(sender);
        }
    }

    public void deleteVetoWinnerEntry(DeXTTAddress sender) {
        // only allow removal if not currently ongoing veto
        if (this.senderLock.get(sender) == null && this.senderInvalid.get(sender) == null && this.vetoT1.get(sender) == null && this.vetoSignature.get(sender) == null && this.vetoFinalized.get(sender) == null) {
            this.vetoWinner.remove(sender);
        }
    }

    public void executeMintTransaction(MintTransaction transaction) {
        if (transaction.getBitcoinTransactionSender().equals(MINTING_ADDRESS) // check if TX sender is allowed to do so
                && (transaction.getAmount().compareTo(BigInteger.valueOf(0)) == 1)) { // amount > 0
            this.balances.compute(transaction.getReceiver(), (k,v) -> (v == null) ? transaction.getAmount() : v.add(transaction.getAmount()));
            this.totalSupply = this.totalSupply.add(transaction.getAmount());
            logger.info(this.chainInfo + " Minted " + transaction.getAmount() + " tokens to " + transaction.getReceiver());
        }
    }

    public void executeClaimContestTransaction(ClaimTransaction transaction) throws UnconfirmedTransactionExecutionException {
        if (transaction.getConfirmations() <= 0) { // check if unconfirmed -> do NOT Execute, throw exception, but emit event
            BigInteger alphaData = new BigInteger(1, Cryptography.alphaData(transaction.getPoiFull().getPoiData()));
            logger.info(this.chainInfo + " Unconfirmed Claim/Contest transaction: " + alphaData);
            UnconfirmedClaimContestTransactionEvent unconfirmedClaimContestTransactionEvent = new UnconfirmedClaimContestTransactionEvent(transaction);
            this.localEventBus.post(unconfirmedClaimContestTransactionEvent);
            GlobalEventBus.getInstance().getEventBus().post(unconfirmedClaimContestTransactionEvent);
            throw new UnconfirmedTransactionExecutionException("Transaction can not be executed without being confirmed in a block.");
        }

        this.contest(transaction.getPoiFull(), transaction.getBitcoinTransactionSender(), transaction.getTxTime());
    }

    public void executeContestHashReferenceTransaction(ContestTransaction transaction) throws UnconfirmedTransactionExecutionException, FullClaimMissingException {
        if (transaction.getConfirmations() <= 0) { // check if unconfirmed -> do NOT Execute, throw exception, but emit event
            logger.info(this.chainInfo + " Unconfirmed Contest Reference transaction: " + transaction.getPoiHashFull());
            GlobalEventBus.getInstance().getEventBus().post(new UnconfirmedContestReferenceTransactionEvent(transaction));
            throw new UnconfirmedTransactionExecutionException("Transaction can not be executed without being confirmed in a block.");
        }

        BigInteger poiHash = transaction.getPoiHashFull();
        ProofOfIntentFull poi = this.ongoingPois.get(poiHash);
        ProofOfIntentFull poiVeto = this.ongoingVetoPoi.get(poiHash);

        if (poi != null) {
            this.contest(poi, transaction.getBitcoinTransactionSender(), transaction.getTxTime());
        } else if (poiVeto != null) {
            this.contest(poiVeto, transaction.getBitcoinTransactionSender(), transaction.getTxTime());
        } else {
            // client should try again after other TXs of that block have been execute (full claim missing)
            // can/should only happen if unconfirmed transactions are supported!! -> full claim and reference-contest in same block (order not deterministic)
            throw new FullClaimMissingException("No claim data available yet!");
        }
    }

    private void contest(ProofOfIntentFull poiFull, DeXTTAddress transactionSender, Date txTime) {
        if (!this.verifyPoi(poiFull, txTime)) {
            return; // or exception?
        }

        BigInteger alphaData = new BigInteger(1, Cryptography.alphaData(poiFull.getPoiData()));
        if (!this.senderInvalid.getOrDefault(poiFull.getPoiData().getSender(), false)) { // decide if contest or veto-contest
            // contest
            if (poiFull.getPoiData().getEndTime().after(txTime)) {
                BigInteger contestSignature = Cryptography.contestantSignature(transactionSender, alphaData);
                if (this.contestSignature.getOrDefault(alphaData, BigInteger.valueOf(0)).compareTo(contestSignature) < 0) { // currentbest < new signature
                    this.contestSignature.put(alphaData, contestSignature);
                    this.contestWinner.put(alphaData, transactionSender);
                }
                // emit contestParticipated event
                ContestParticipatedEvent contestParticipatedEvent = new ContestParticipatedEvent(alphaData, poiFull, transactionSender);
                this.localEventBus.post(contestParticipatedEvent);
                this.globalEventBus.getEventBus().post(contestParticipatedEvent);
                logger.info(this.chainInfo + " Contest participated: " + alphaData + " by " + transactionSender);
            }
        } else {
            // veto-contest
            DeXTTAddress sender = poiFull.getPoiData().getSender();
            if (this.vetoT1.getOrDefault(sender, Date.from(Instant.EPOCH)).after(txTime)) {
                BigInteger vetoSignature = Cryptography.vetoSignature(transactionSender, sender);
                if (this.vetoSignature.getOrDefault(sender, BigInteger.valueOf(0)).compareTo(vetoSignature) < 0) { // currentbest < new signature
                    this.vetoSignature.put(sender, vetoSignature);
                    this.vetoWinner.put(sender, transactionSender);
                }
                logger.info(this.chainInfo + " Veto-Contest participated: " + sender + " by " + transactionSender);
            }
        }
    }

    // called from claim & contest -> checks stuff && sets senderLock, start contest and veto-contest
    private boolean verifyPoi(ProofOfIntentFull poiFull, Date txTime) {
        ProofOfIntentData poi = poiFull.getPoiData();

        // signatures are actually already checked when matching transactions
        BigInteger alphaData = new BigInteger(1, Cryptography.alphaData(poi));

        if (alphaData.equals(this.senderLock.get(poi.getSender()))) {
            return true; // already registered claim, just do contest participation
        }

        if (!Cryptography.verifySigA(poi, poiFull.getSigA())) {
            return false;
        }

        if (this.senderLock.get(poi.getSender()) == null) {
            // first sighting of a new Claim for sender, start contest

            // check poi first
            if (!Cryptography.verifySigB(poiFull.getSigA(), poi.getReceiver(), poiFull.getSigB())
                    || poi.getStartTime().after(txTime) || poi.getEndTime().before(txTime)
                    || poi.getAmount().compareTo(WITNESS_REWARD) <= 0 || this.balanceOf(poi.getSender()).compareTo(poi.getAmount()) < 0) {
                return false;
            }

            this.ongoingPois.put(alphaData, poiFull);
            this.senderLock.put(poi.getSender(), alphaData);

            // emit event -> contest started -> clients can start listening for blocks with endtime
            ContestStartedEvent contestStartedEvent = new ContestStartedEvent(alphaData, poiFull);
            this.localEventBus.post(contestStartedEvent);
            this.globalEventBus.getEventBus().post(contestStartedEvent);
            logger.info(this.chainInfo + " Started Contest: " + alphaData);

        } else if (!this.senderInvalid.getOrDefault(poi.getSender(), false)) {
            // first conflicting PoI
            this.senderInvalid.put(poi.getSender(), true);
            this.balances.put(poi.getSender(), BigInteger.valueOf(0));
            this.ongoingVetoPoi.put(alphaData, poiFull);

            BigInteger originalAlphaData = this.senderLock.get(poi.getSender());
            ProofOfIntentFull originalPoiFull = this.ongoingPois.get(originalAlphaData);
            ProofOfIntentData originalPoi = originalPoiFull.getPoiData();

            Date vetoEndTime = Helper.calculateVetoEndTime(originalPoi, poi);
            vetoT1.put(poi.getSender(), vetoEndTime);

            // emit event -> veto contest started -> clients can start listening for block with endtime
            VetoContestStartedEvent vetoContestStartedEvent = new VetoContestStartedEvent(originalAlphaData, originalPoiFull, alphaData, poiFull, vetoEndTime);
            this.localEventBus.post(vetoContestStartedEvent);
            this.globalEventBus.getEventBus().post(vetoContestStartedEvent);
            logger.info(this.chainInfo + " Started Veto-Contest: " + poi.getSender());
        } else {
            // another conflicting PoI -> save for reference transactions
            this.ongoingVetoPoi.put(alphaData, poiFull);
        }

        return true;
    }

    public void executeFinalizeTransaction(FinalizeTransaction transaction) {
        ProofOfIntentFull poi = this.ongoingPois.get(transaction.getPoiHashFull());

        if (poi != null) {
            DeXTTAddress sender = poi.getPoiData().getSender();
            if (!this.senderInvalid.getOrDefault(sender, false)) {
                BigInteger alphaData = this.senderLock.get(sender);
                if (alphaData != null && alphaData.equals(transaction.getPoiHashFull())) {
                    Date t1 = poi.getPoiData().getEndTime();
                    if (t1.compareTo(transaction.getTxTime()) <= 0) {
                        DeXTTAddress winner = this.contestWinner.get(alphaData);
                        BigInteger rawValue = poi.getPoiData().getAmount().subtract(WITNESS_REWARD);

                        this.balances.computeIfPresent(sender, (k,v) -> v.subtract(poi.getPoiData().getAmount()));
                        this.balances.compute(poi.getPoiData().getReceiver(), (k,v) -> v != null ? v.add(rawValue) : rawValue);
                        this.balances.compute(winner, (k,v) -> v != null ? v.add(WITNESS_REWARD) : WITNESS_REWARD);

                        this.senderLock.put(sender, null);

                        TransferFinalizedEvent transferFinalizedEvent = new TransferFinalizedEvent(alphaData, poi);
                        this.localEventBus.post(transferFinalizedEvent);
                        this.globalEventBus.getEventBus().post(transferFinalizedEvent);
                        logger.info(this.chainInfo + " Contest Finalized: " + alphaData);
                    }
                }
            }
        }
    }

    public void executeFinalizeVetoTransaction(FinalizeVetoTransaction transaction) {
        DeXTTAddress poiSender = transaction.getConflictingPoiSender();

        if (this.senderInvalid.getOrDefault(poiSender, false)) {
            Date t1 = this.vetoT1.get(poiSender);
            if (t1 != null && !this.vetoFinalized.getOrDefault(poiSender, false) && t1.compareTo(transaction.getTxTime()) <= 0) {
                DeXTTAddress winner = this.vetoWinner.get(poiSender);

                this.balances.compute(winner, (k,v) -> v != null ? v.add(WITNESS_REWARD) : WITNESS_REWARD);

                this.vetoFinalized.put(poiSender, true);

                VetoFinalizedEvent vetoFinalizedEvent = new VetoFinalizedEvent(poiSender, t1);
                this.localEventBus.post(vetoFinalizedEvent);
                this.globalEventBus.getEventBus().post(vetoFinalizedEvent);
                logger.info(this.chainInfo + " Veto-Contest finalized: " + poiSender);
            }
        }
    }
}
