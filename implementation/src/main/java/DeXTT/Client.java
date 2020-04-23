package DeXTT;

import Communication.RMI.ProofOfIntentRMI;
import Configuration.Configuration;
import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.DataStructure.ProofOfIntentFull;
import DeXTT.Exception.FullClaimMissingException;
import DeXTT.Exception.UnconfirmedTransactionExecutionException;
import DeXTT.Exception.BitcoinParseException;
import DeXTT.Transaction.Bitcoin.BitcoinTransaction;
import DeXTT.Transaction.MintTransaction;
import DeXTT.Transaction.Transaction;
import DeXTT.Transaction.Bitcoin.RawBitcoinTransaction;
import Communication.Bitcoin.BitcoinCommunicator;
import Events.*;
import Runners.Evaluator;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.ECKeyPair;
import wf.bitcoin.javabitcoindrpcclient.GenericRpcException;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.time.Instant;
import java.util.*;

import static Configuration.Constants.MINTING_ADDRESS;
import static DeXTT.Cryptography.createAddressFromWIFPrivateKey;
import static DeXTT.Cryptography.createSigningKeyFromWIFPrivateKey;

public class Client {

    private static final Logger logger = LogManager.getLogger();
    private Configuration configuration;

    private Wallet wallet;
    private BitcoinCommunicator communicator;
    private TransactionManager transactionManager;
    private GlobalEventBus globalEventBus;
    private EventBus localEventBus;

    private int latestReadBlock = -1;
    private DeXTTAddress deXTTAddress;
    private ECKeyPair keyPair;
    private String urlRPC;

    private Map<BigInteger, ProofOfIntentFull> contestsStarted;
    private Set<BigInteger> contestsFinalized;
    private Set<DeXTTAddress> vetoContestsFinalized;

    public Client(String urlRPC) throws MalformedURLException, GenericRpcException {
        this.urlRPC = urlRPC;
        this.communicator = new BitcoinCommunicator(urlRPC);
        this.configuration = Configuration.getInstance();
        this.globalEventBus = GlobalEventBus.getInstance();
        this.localEventBus = new EventBus();
        this.wallet = new Wallet(localEventBus, urlRPC);
        this.transactionManager = new TransactionManager();
        this.deXTTAddress = this.calculateAddress();
        if (this.deXTTAddress == null) {
            throw new GenericRpcException("Received private key could not be used to derive DeXTT Address!");
        }
        this.keyPair = this.calculateKeyPair();

        this.contestsStarted = new HashMap<>();
        this.contestsFinalized = new HashSet<>();
        this.vetoContestsFinalized = new HashSet<>();

        this.localEventBus.register(this);
    }

    public void initialize() {
        this.initializeWallet();
    }

    public synchronized void sendDeXTTTransaction(Transaction transaction) throws GenericRpcException {
        List<BitcoinTransaction> transactions = transaction.convertToDeXTTBitcoinTransactions();
        String txName = transaction.getClass().getSimpleName();
        long txSize = 0;
        for (BitcoinTransaction tx: transactions) {
            byte[] payload = tx.convertToDeXTTPayload();
            long sentSize = this.communicator.sendDeXTTTransaction(payload);
            if (sentSize > 0) {
                txSize += sentSize;
            }
        }
        if (txSize > 0) {
            Evaluator.getInstance().addTransactionSizeEntry(txName, txSize);
        }
    }

    /**
     * Reads & processes all blocks from DeXTT-Genesis to latest.
     */
    private void initializeWallet() throws GenericRpcException {
        int latestBlock = this.communicator.getBlockCount();

        this.processBlocks(this.configuration.getGenesisBlockHeight(), latestBlock);
    }

    /**
     *
     * @param startBlock    inclusive, start with this block
     * @param latestBlock   inclusive, end with this block
     */
    private void processBlocks(int startBlock, int latestBlock) throws GenericRpcException {
        List<RawBitcoinTransaction> transactions = this.communicator.filterBlocksForDeXTT(startBlock, latestBlock);
        List<RawBitcoinTransaction> scheduledAfterBlock = new ArrayList<>();
        Date currentBLockTime = Date.from(Instant.EPOCH);
        for (RawBitcoinTransaction rawTransaction: transactions) {
            if (rawTransaction.getTime().after(currentBLockTime)) {
                // next block -> check scheduledAfterBlock txs
                for (RawBitcoinTransaction scheduledTransaction: scheduledAfterBlock) {
                    try {
                        this.processRawTransaction(scheduledTransaction);
                    } catch (FullClaimMissingException e) {
                        // ignore, do NOT try again, block has been processed, tx cannot be executed
                    }
                }

                scheduledAfterBlock.clear();
                currentBLockTime = rawTransaction.getTime();
            }

            if (!configuration.processUnconfirmedTransactions() || !this.transactionManager.containsProcessedUnconfirmedTxId(rawTransaction.getTxId())) {
                try {
                    this.processRawTransaction(rawTransaction);
                } catch (FullClaimMissingException e) {
                    // try execution again after block has been processed
                    scheduledAfterBlock.add(rawTransaction);
                }
            } else {
                // remove from processedUnconfirmedTxIds -> no longer unconfirmed, was in block
                this.transactionManager.removeProcessedUnconfirmedTxId(rawTransaction.getTxId()); // ok if it did not contain it
            }
        }

        for (RawBitcoinTransaction scheduledTransaction: scheduledAfterBlock) {
            // if happened in last block, rescheduling not yet triggered -> execute now
            try {
                this.processRawTransaction(scheduledTransaction);
            } catch (FullClaimMissingException e) {
                // ignore, do NOT try again, block has been processed, tx cannot be executed
            }
        }
        scheduledAfterBlock.clear();

        this.latestReadBlock = latestBlock;

        Date latestTime = this.communicator.getBlockTime(this.latestReadBlock);
        this.globalEventBus.getEventBus().post(new NewBlockFoundEvent(this.urlRPC, latestTime));
    }

    /**
     *
     * @param rawTransaction
     * @return  true, if TX was processed. false otherwise
     */
    private boolean processRawTransaction(RawBitcoinTransaction rawTransaction) throws FullClaimMissingException {
        BitcoinTransaction bitcoinTransaction;
        try {
            bitcoinTransaction = BitcoinParser.parseBitcoinDeXTTTransaction(rawTransaction);
        } catch (BitcoinParseException e) {
            // no valid DeXTT transaction, but still counts as processed (as in: Does not have to be processed any time later)
            return true;
        } catch (UnconfirmedTransactionExecutionException e) {
            // Counts as NOT processed -> needs to be processed later with block time available
            return false;
        }

        BigInteger poiHash = bitcoinTransaction.getPoiHashShort();
        List<Transaction> transactions;
        if (poiHash != null) {
            // search for unfinished TXs with same poiHash in TXManager
            transactions = bitcoinTransaction.putIntoDeXTTTransaction(this.transactionManager.getMatchingTransactions(poiHash));
        } else {
            // TX lives on its own, just create and execute
            transactions = new ArrayList<>();
            transactions.add(bitcoinTransaction.createNewCorrespondingDeXTTTransaction());
        }

        boolean ret = true;
        boolean throwFullClaimMissing = false;
        for (Transaction transaction : transactions) {
            if (transaction.isComplete()) {
                // Execute
                try {
                    transaction.tryToExecute(this.wallet);
                    if (poiHash != null) {
                        this.transactionManager.markAsCompleted(poiHash, transaction);
                    }
                } catch (UnconfirmedTransactionExecutionException e) {
                    // Counts as NOT processed -> needs to be processed later with block time available
                    // but is already added into TXs... => trying to add again will throw AlreadyAddedTransactionException
                    if (poiHash != null) {
                        if (!transaction.resetToConfirmationsOnly()) { // try delete only unconfirmed parts -> keep already confirmed parts
                            this.transactionManager.markAsCompleted(poiHash, transaction); // delete TX, create new one from block later, not needed anymore!!!
                        }
                    }
                    ret = false;
                } catch (FullClaimMissingException e) {
                    // reschedule for execution after block has been handled
                    throwFullClaimMissing = true;
                }

            } else if (poiHash != null) {
                // incomplete, add to tx manager, could be newly created
                this.transactionManager.addIncompleteTransaction(poiHash, transaction);
                ret = ret && transaction.canBeExecutedUnconfirmed(); // false if either was false or can not be executed
            } else {
                // should not happen!!!
                logger.debug("DeXTT Transaction without PoI not ready to execute, should not happen!");
            }
        }

        if (throwFullClaimMissing) {
            throw new FullClaimMissingException();
        }

        return ret;
    }

    public void readLatestBlocks() throws GenericRpcException {
        int latestBlock = this.communicator.getBlockCount();

        if (latestBlock > this.latestReadBlock) {
            this.processBlocks(this.latestReadBlock + 1, latestBlock);

            if (configuration.processUnconfirmedTransactions()) {
                this.transactionManager.clearReadUnconfirmedTxIds(); // new block arrived, start from scratch; otherwise all TxIds in block would need to be removed
            }
        }
    }

    public void readLatestUnconfirmedTXs() throws GenericRpcException {
        if (!this.configuration.processUnconfirmedTransactions()) {
            return;
        }

        Set<String> newUnconfirmedTxIds = this.communicator.getNewTxIdsSinceLastBlock(this.transactionManager.getReadUnconfirmedTxIds());
        this.transactionManager.addReadUnconfirmedTxIds(newUnconfirmedTxIds);

        for (String txId: newUnconfirmedTxIds) {
            if (!this.transactionManager.containsProcessedUnconfirmedTxId(txId)) {
                // parse
                RawBitcoinTransaction rawBitcoinTransaction = this.communicator.filterTxIdForDeXTT(txId);
                if (rawBitcoinTransaction != null) {
                    // process
                    rawBitcoinTransaction.setTime(this.communicator.getBlockTime(this.latestReadBlock)); // add time of latest read block
                    try {
                        if (this.processRawTransaction(rawBitcoinTransaction)) { // only mark as processed if TX was processed successfully
                            this.transactionManager.addProcessedUnconfirmedTxId(txId);
                        }
                    } catch (FullClaimMissingException e) {
                        // actually should not happen with unconfirmed TXs, ignore
                    }
                }
            }
        }
    }

    @Subscribe
    public synchronized void contestStartedEvent(ContestStartedEvent event) {
//        synchronized (this.contestsStarted) {
        this.contestsStarted.put(event.getAlphaData(), event.getPoi());
//        }
    }

    @Subscribe
    public void transferFinalizedEvent(TransferFinalizedEvent event) {
        this.contestsFinalized.add(event.getAlphaData());
    }

    @Subscribe
    public void vetoContestStartedEvent(VetoContestStartedEvent event) {
        this.vetoContestsFinalized.remove(event.getOriginalPoi().getPoiData().getSender());
    }

    @Subscribe
    public void vetoFinalizedEvent(VetoFinalizedEvent event) {
        this.vetoContestsFinalized.add(event.getConflictingPoiSender());

        if (this.configuration.isEnableAutoUnlocking()) {
            // make sender valid again -> for testing purposes, not protocol conform!!!
            this.wallet.unlock(event.getConflictingPoiSender());
            this.wallet.makeSenderValid(event.getConflictingPoiSender());
            if (this.deXTTAddress.equals(MINTING_ADDRESS)) {
                // not guaranteed to work (be consistent on all chains), could be before vetoFinalize in a block on earlier chains
                MintTransaction mintTransaction = new MintTransaction(event.getConflictingPoiSender(), BigInteger.valueOf(100));
                this.sendDeXTTTransaction(mintTransaction);
            }
        }
    }

    @Subscribe
    public synchronized void unconfirmedClaimContestTransactionEvent(UnconfirmedClaimContestTransactionEvent event) {
        if (this.configuration.processUnconfirmedTransactions()) {
            ProofOfIntentFull poiFull = event.getClaimTransaction().getPoiFull();
            BigInteger poiHash = Cryptography.calculateFullPoiHash(poiFull.getPoiData());

//            synchronized (this.contestsStarted) {
            this.contestsStarted.put(poiHash, poiFull);
//            }
        }
    }

    // check wallet related stuff (balance of sender, senderlock)
    public boolean checkPoIForClaim(ProofOfIntentRMI poi) {
        if (this.wallet.balanceOf(poi.getSender()).compareTo(poi.getAmount()) < 0) {
            return false;
        }

        if (!this.configuration.isAllowDoubleSpend()) { // only check lockStatus if double spending is not allowed to be sent to the network
            return this.wallet.lockStatus(poi.getSender()) == null;
        } else {
            return true;
        }
    }

    public Date getTimeOfLastBlockInChain() {
        return this.communicator.getBlockTime(this.communicator.getBlockCount());
    }

    private DeXTTAddress calculateAddress() {
        String privateKey = this.communicator.getPrivateKey();
        DeXTTAddress address = createAddressFromWIFPrivateKey(privateKey);
        return address;
    }

    private ECKeyPair calculateKeyPair() throws GenericRpcException {
        String privateKey = this.communicator.getPrivateKey();
        ECKeyPair keyPair = createSigningKeyFromWIFPrivateKey(privateKey);
        return keyPair;
    }

    public DeXTTAddress getDeXTTAddress() {
        return deXTTAddress;
    }

    public ECKeyPair getKeyPair() {
        return this.keyPair;
    }

    public String getUrlRPC() {
        return urlRPC;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public synchronized boolean hasContestStarted(BigInteger poiHash) {
//        synchronized (this.contestsStarted) {
        ProofOfIntentFull poi = this.contestsStarted.get(poiHash);
        return poi != null;
//        }
    }

    public Set<BigInteger> getContestsFinalized() {
        return contestsFinalized;
    }

    public Set<DeXTTAddress> getVetoContestsFinalized() {
        return vetoContestsFinalized;
    }
}
