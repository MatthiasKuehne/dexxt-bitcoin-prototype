package DeXTT;

import Communication.RMI.ProofOfIntentRMI;
import Communication.RMI.RMIProvider;
import Configuration.Configuration;
import DeXTT.DataStructure.*;
import DeXTT.Exception.PoINotStartedException;
import DeXTT.Transaction.ClaimTransaction;
import DeXTT.Transaction.FinalizeTransaction;
import DeXTT.Transaction.FinalizeVetoTransaction;
import DeXTT.Transaction.Transaction;
import Events.*;
import com.google.common.eventbus.Subscribe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import wf.bitcoin.javabitcoindrpcclient.GenericRpcException;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static Configuration.Constants.*;

public class ClientsService {

    private static final Logger logger = LogManager.getLogger();
    private Configuration configuration;

    private List<Client> clients = new ArrayList<>();
    private RMIProvider rmiProvider;
    private ContestManager contestManager;

    private DeXTTAddress deXTTAddress;
    private ECKeyPair keyPair;
    private ScheduledExecutorService executorService;
    private Map<String, Boolean> clientsAccessible;

    public ClientsService() throws AlreadyBoundException, RemoteException, GenericRpcException, MalformedURLException {
        this.configuration = Configuration.getInstance();

        for (String url: configuration.getUrlRPC()) {
            Client client = new Client(url);
            clients.add(client);

            if (this.deXTTAddress == null) {
                this.deXTTAddress = client.getDeXTTAddress();
            }
            if (this.keyPair == null) {
                this.keyPair = client.getKeyPair();
            }
        }

        this.contestManager = new ContestManager();

        // start RMI stuff
        this.rmiProvider = new RMIProvider();
        this.rmiProvider.startRMIServer(this.deXTTAddress);

        // start EventBus stuff
        GlobalEventBus globalEventBus = GlobalEventBus.getInstance();
        globalEventBus.getEventBus().register(this);

        this.executorService = Executors.newScheduledThreadPool(10);
        this.clientsAccessible = new HashMap<>();
    }

    public void initialize() throws GenericRpcException {
        for (Client client: this.clients) {
            client.initialize();
            this.clientsAccessible.put(client.getUrlRPC(), true);
        }
    }

    public void close() {
        try {
            this.rmiProvider.stopRMIServer(this.deXTTAddress);
        } catch (RemoteException | NotBoundException e) {
            // ignore, shutting down anyways
        }

        // shutdown executorservice: recommended way from Documentation (https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html)
        this.executorService.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!this.executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                this.executorService.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!this.executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.error("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            this.executorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    public void loopIteration() {
        // get new blocks
        for (Client client: this.clients) {
            try {
                client.readLatestBlocks(); // nothing to handle, ready TXs are executed automatically
                this.clientsAccessible.put(client.getUrlRPC(), true);
            } catch (GenericRpcException e) {
                if (this.clientsAccessible.get(client.getUrlRPC())) {
                    this.clientsAccessible.put(client.getUrlRPC(), false);
                    logger.info("[" + client.getUrlRPC() + "] Blockchain communication error.", e);
                }
            }

            if (configuration.processUnconfirmedTransactions()) {
                try {
                    client.readLatestUnconfirmedTXs();
                } catch (GenericRpcException e) {
                    // do nothing, not really a problem for unconfirmed TXs
                }
            }
        }

        for (ProofOfIntentRMI poi: this.rmiProvider.removeUnhandledPoIs()) {
            try {
                Transaction claimTransaction = this.createTransactionFromRMIPoi(poi);
                if (claimTransaction != null) {
                    logger.info("Sending Claim Transaction from RMI data.");
                    this.sendDeXTTTransaction(claimTransaction);
                }
            } catch (PoINotStartedException e) {
                // try again later -> add again
                logger.info("RMI PoI not started yet, trying again later.");
                this.rmiProvider.addPoI(poi);
            }
        }
    }

    private void sendDeXTTTransaction(Transaction transaction) {
        for (Client client: this.clients) {
            client.sendDeXTTTransaction(transaction);
        }
    }

    private Transaction createTransactionFromRMIPoi(ProofOfIntentRMI poi) throws PoINotStartedException {
        // check sigA
        ProofOfIntentData poiData = new ProofOfIntentData(poi.getSender(), poi.getReceiver(), poi.getAmount(), poi.getStartTime(), poi.getEndTime());

        if (poiData.getSender() != null && poiData.getReceiver() != null && poiData.getAmount() != null && poiData.getStartTime() != null && poiData.getEndTime() != null
                && poiData.getReceiver().equals(this.deXTTAddress)
                && poiData.getEndTime().after(Date.from(Instant.now())) && poiData.getAmount().compareTo(WITNESS_REWARD) > 0
                && Cryptography.verifySigA(poiData, poi.getSigA())) {

            if (!poiData.getStartTime().before(Date.from(Instant.now()))
                    || this.contestManager.getEarliestBlockTime().compareTo(poiData.getStartTime()) < 0) { // make sure there exist blocks on each chain with blocktime t >= t0 => claim will be in block with t > t0
                throw new PoINotStartedException("PoI is not started yet (start time).");
            }

            boolean clientsOk = true;
            for (Client client: this.clients) {
                if (!client.checkPoIForClaim(poi)) {
                    clientsOk = false;
                }
            }

            if (clientsOk) {
                // first sign tx -> create sigB
                Sign.SignatureData sigB = Cryptography.signAlphaData(poi.getSigA(), this.keyPair);
                BigInteger poiHashShort = Cryptography.calculateShortPoiHash(poiData);

                Transaction transaction = new ClaimTransaction(poiData, poi.getSigA(), sigB, poiHashShort);

                return transaction;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private long calculateMaximumContestWaitTime(ProofOfIntentData poiData) {
        long transactionDuration = poiData.getEndTime().getTime() - poiData.getStartTime().getTime();
        long currentTimeUpperBound = Math.max(this.contestManager.getLatestKnownBlockTime().getTime(), Date.from(Instant.now()).getTime());
        long maximumWaitTime = (long) (transactionDuration * maxContestWaitPercentage - (currentTimeUpperBound - poiData.getStartTime().getTime()));
        return maximumWaitTime;
    }

    @Subscribe
    public void newBlockFoundEvent(NewBlockFoundEvent event) {
        logger.info("[" + event.getRpcUrl() + "] Processed up to Block with time: " + event.getBlockTime());

        this.contestManager.setNewBlockTime(event);

        // check if contest participation needs to be sent
        Iterator<Map.Entry<BigInteger, ProofOfIntentFull>> startedContestsIterator = this.contestManager.getStartedContests().entrySet().iterator();
        while(startedContestsIterator.hasNext()) {
            Map.Entry<BigInteger, ProofOfIntentFull> entry = startedContestsIterator.next();
            startedContestsIterator.remove();
            if (!this.contestManager.participatedIn(entry.getKey()) // not participated yet
                    && Date.from(Instant.now()).before(entry.getValue().getPoiData().getEndTime())) { // current local time < t1 (end time)
                // not participated yet -> do it now!!
                this.contestManager.addContestParticipation(entry.getKey(), entry.getValue());

                long maximumWaitTime = this.calculateMaximumContestWaitTime(entry.getValue().getPoiData());
                if (this.configuration.processUnconfirmedTransactions()) {
                    // participate (unconfirmed txs)
                    ContestParticipationTask runner = new ContestParticipationTask(entry.getKey(), entry.getValue(), this.contestManager, this.deXTTAddress, this);
                    long bound = Math.min(Math.max(maximumWaitTime, 0), this.configuration.getMaxContestWaitMilliseconds());
                    long waitTime = 0;
                    if (bound > 0) {
                        waitTime = ThreadLocalRandom.current().nextLong(bound);
                    }
                    logger.info("Participating in Contest for " + entry.getKey() + " in " + waitTime + " milliseconds.");
                    this.executorService.schedule(runner, waitTime, TimeUnit.MILLISECONDS);
                } else {
                    // participate (only confirmed txs)
                    long stopAt = System.currentTimeMillis() + maximumWaitTime;
                    int blockCountToWait = ThreadLocalRandom.current().nextInt(this.configuration.getMaxContestBlocksWait() + 1);
                    logger.info("Participating in Contest for " + entry.getKey() + " in " + blockCountToWait + " blocks.");
                    ContestParticipationTask runner = new ContestParticipationTask(entry.getKey(), entry.getValue(), this.contestManager, this.deXTTAddress, this, stopAt, blockCountToWait);
                    this.executorService.execute(runner); // run immediately -> internally waits
                }
            }
        }

        // check for transactions to finalize
        Iterator<PoITimeHash> readyToFinalize = this.contestManager.getPoisEndedBefore(event.getBlockTime()).iterator();
        while (readyToFinalize.hasNext()) {
            PoITimeHash p = readyToFinalize.next();
            // first check if all wallets have time > T1...
            if (!this.isAfterBlockChainTime(p.getEndTime()) && !this.isAlreadyFinalized(p.getPoiHash())) {
                logger.info("Finalizing poi: " + p.getPoiHash());
                FinalizeTransaction transaction = new FinalizeTransaction(p.getPoiHash());
                this.sendDeXTTTransaction(transaction);
                readyToFinalize.remove();
            } else if (this.isAlreadyFinalized(p.getPoiHash())) {
                readyToFinalize.remove();
            }
        }

        // check if Finalize-Veto Transactions ready
        Iterator<VetoFinalizeData> readyToFinalizeVeto = this.contestManager.getVetoEndedBefore(event.getBlockTime()).iterator();
        while (readyToFinalizeVeto.hasNext()) {
            VetoFinalizeData v = readyToFinalizeVeto.next();
            // first check if all wallets have time > vetoT1...
            if (!this.isAfterBlockChainTime(v.getEndTime()) && !this.isAlreadyVetoFinalized(v.getConflictingPoiSender())) {
                // check if veto winner, ignore inconsistencies, send TX to all chains if this is winner on at least one, failsafe...
                for (Client client: this.clients) {
                    if (this.deXTTAddress.equals(client.getWallet().vetoWinner(v.getConflictingPoiSender()))) {
                        // this is the winner -> send Finalize-Veto for this client
                        logger.info("Veto-Finalizing poi from: " + v.getConflictingPoiSender());
                        FinalizeVetoTransaction transaction = new FinalizeVetoTransaction(v.getConflictingPoiSender());
                        this.sendDeXTTTransaction(transaction);
                        break; // only send once!!!
                    }
                }
                readyToFinalizeVeto.remove();
            } else if (this.isAlreadyVetoFinalized(v.getConflictingPoiSender())) {
                readyToFinalizeVeto.remove();
            }
        }
    }

    /**
     * Called once for each Claim/PoI/Contest for each Blockchain
     * @param event
     */
    @Subscribe
    public void contestStartedEvent(ContestStartedEvent event) {
        if (!this.isAlreadyFinalized(event.getAlphaData())) { // not finalized
            logger.debug("Contest started event: " + event.getAlphaData());
            if (event.getPoi().getPoiData().getReceiver().equals(this.deXTTAddress)) {
                // this is the receiver of the transfer -> has to send finalize TX at t > t1
                // Claim is already participation -> just save times

                this.contestManager.addPoiEndTime(event.getPoi().getPoiData().getEndTime(), event.getAlphaData());
            } else if (!this.contestManager.participatedIn(event.getAlphaData()) && this.isAfterBlockChainTime(event.getPoi().getPoiData().getEndTime())) { // not participated AND Endtime after current Blockchain time
                // not receiver, no need to save endtime, but participate in contest
                this.contestManager.addStartedContest(event.getAlphaData(), event.getPoi()); // save to check in "newBLockFoundEvent", is triggered afterwards
            }
        }
    }

    @Subscribe
    public void transferFinalizedEvent(TransferFinalizedEvent event) {
        // remove end time -> has already been finalized
        logger.debug("Transfer finalized event: " + event.getPoi());
        this.contestManager.deletePoiEndTime(new PoITimeHash(event.getPoi().getPoiData().getEndTime(), event.getAlphaData()));
    }

    @Subscribe
    public void vetoContestStartedEvent(VetoContestStartedEvent event) {
        if (!this.isAlreadyVetoFinalized(event.getOriginalPoi().getPoiData().getSender())) {
            logger.debug("Veto-Contest started event: " + event.getOriginalPoi().getPoiData().getSender());
            this.contestManager.addVetoEndTime(event.getVetoEndTime(), event.getOriginalPoi().getPoiData().getSender());
        }
        this.contestManager.deletePoiEndTime(new PoITimeHash(event.getOriginalPoi().getPoiData().getEndTime(), event.getOriginalAlphaData())); // always delete, otherwise finalize is sent
    }

    @Subscribe
    public void contestParticipatedEvent(ContestParticipatedEvent event) {
        if (isAfterBlockChainTime(event.getPoi().getPoiData().getEndTime())) { // check if contestParticipants even needed -> can be ignored if contest is already over
            logger.debug("Contest participation event for " + event.getAlphaData() + " from " + event.getParticipant());
            this.contestManager.addContestParticipant(event.getAlphaData(), event.getParticipant());
            if (event.getParticipant().equals(this.deXTTAddress)) {
                this.contestManager.addContestParticipation(event.getAlphaData(), event.getPoi());
            }
        }
    }

    @Subscribe
    public void vetoFinalizedEvent(VetoFinalizedEvent event) {
        // remove end time -> has already been veto-finalized
        logger.debug("Sender veto-finalized: " + event.getConflictingPoiSender());
        this.contestManager.deleteVetoFinalizeDate(new VetoFinalizeData(event.getVetoEndTime(), event.getConflictingPoiSender()));
    }

    @Subscribe
    public void unconfirmedClaimContestTransactionEvent(UnconfirmedClaimContestTransactionEvent event) {
        if (this.configuration.processUnconfirmedTransactions()
                //&& this.contestManager.getEarliestBlockTime().compareTo(event.getClaimTransaction().getPoiFull().getPoiData().getEndTime()) <= 0) { // check if time of last block is t <= t1 (endtime of TX); otherwise it is not save to trust claim/contest
                && this.contestManager.getLatestKnownBlockTime().compareTo(event.getClaimTransaction().getPoiFull().getPoiData().getEndTime()) <= 0) {
            ProofOfIntentFull poiFull = event.getClaimTransaction().getPoiFull();
            BigInteger poiHash = Cryptography.calculateFullPoiHash(poiFull.getPoiData());

            this.contestManager.addContestParticipant(poiHash, event.getClaimTransaction().getBitcoinTransactionSender());

            if (!this.contestManager.hasContestStarted(poiHash)
                    && !event.getClaimTransaction().getPoiFull().getPoiData().getReceiver().equals(this.deXTTAddress)
                    && !this.contestManager.participatedIn(poiHash)) {
                this.contestManager.addStartedContest(poiHash, poiFull); // not really needed -> is only check to participate later
                // if not receiver -> participate (with delay)

                // set participation immediately...
                this.contestManager.addContestParticipation(poiHash, poiFull);

                long maximumWaitTime = this.calculateMaximumContestWaitTime(poiFull.getPoiData());
                ContestParticipationTask runner = new ContestParticipationTask(poiHash, poiFull, this.contestManager, this.deXTTAddress, this);
                long waitTime = 0;
                long bound = Math.min(Math.max(maximumWaitTime, 0), this.configuration.getMaxContestWaitMilliseconds());
                if (bound > 0) {
                    waitTime = ThreadLocalRandom.current().nextLong(bound);
                }
                logger.info("Participating in Contest for " + poiHash + " in " + waitTime + " milliseconds.");
                this.executorService.schedule(runner, waitTime, TimeUnit.MILLISECONDS);
            }
        }
    }

    @Subscribe
    public void unconfirmedContestReferenceTransactionEvent(UnconfirmedContestReferenceTransactionEvent event) {
        if (this.configuration.processUnconfirmedTransactions()) {
            this.contestManager.addContestParticipant(event.getContestTransaction().getPoiHashFull(), event.getContestTransaction().getBitcoinTransactionSender());
        }
    }

    /**
     * Caches latest time in contest manager
     *
     * @param time
     * @return
     */
    public boolean isAfterBlockChainTime(Date time) {
        synchronized (this.contestManager) {
            if (this.contestManager.isAfterBlockChainTime(time)) {
                // get newest times from chain -> might have changed, now after <time>
                Date latest = Date.from(Instant.EPOCH);
                for (Client client : this.clients) {
                    Date clientTime = client.getTimeOfLastBlockInChain();
                    if (clientTime.after(latest)) {
                        latest = clientTime;
                    }
                }
                this.contestManager.setLatestKnownBlockTime(latest);

                // check again with new time
                return this.contestManager.isAfterBlockChainTime(time);
            } else {
                // no need to redo cached time
                return false;
            }
        }
    }

    /**
     *
     * @param poiHash
     * @return  true if tx is finalized on at least one blockchain/client
     */
    private boolean isAlreadyFinalized(BigInteger poiHash) {
        for (Client client: this.clients) {
            if (client.getContestsFinalized().contains(poiHash)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param conflictingPoiSender
     * @return  true if conflictingPoiSender is veto-finalized on at least one blockchain/client
     */
    private boolean isAlreadyVetoFinalized(DeXTTAddress conflictingPoiSender) {
        for (Client client: this.clients) {
            if (client.getVetoContestsFinalized().contains(conflictingPoiSender)) {
                return true;
            }
        }
        return false;
    }

    public List<Client> getClients() {
        return clients;
    }

    public DeXTTAddress getDeXTTAddress() {
        return deXTTAddress;
    }

    public ECKeyPair getKeyPair() {
        return keyPair;
    }

    public Date getEarliestBlockTime() {
        return this.contestManager.getEarliestBlockTime();
    }

    public BigInteger getBalance(DeXTTAddress address) {
        String latestChain = this.contestManager.getLatestProcessedChain();
        if (latestChain != null) {
            for (Client c : this.clients) {
                if (c.getUrlRPC().equals(latestChain)) {
                    return c.getWallet().balanceOf(address);
                }
            }
        }
        return BigInteger.ZERO;
    }

    public BigInteger getMinimumBalance(DeXTTAddress address) {
        BigInteger minimumBalance = null;
        for (Client client: this.clients) {
            BigInteger bal = client.getWallet().balanceOf(address);
            if (minimumBalance == null) {
                minimumBalance = bal;
            }
            if (minimumBalance.compareTo(bal) > 0) {
                minimumBalance = bal;
            }
        }
        return minimumBalance;
    }

    public ProofOfIntentRMI createRandomPoi(long dexxtTransactionTimeSeconds) {
        // check own balance first
        BigInteger balance = this.getMinimumBalance(this.deXTTAddress);
        BigInteger minimumAmount = WITNESS_REWARD.add(BigInteger.ONE);
        if (balance.compareTo(minimumAmount) < 0) { // minimum of 1 + Witness reward tokens required (1 for transfer)
            return null;
        }

        List<DeXTTAddress> clients = this.configuration.getClientAddresses();
        boolean found = false;
        DeXTTAddress receiver = null;
        while (!found) {
            int clientIndex = ThreadLocalRandom.current().nextInt(clients.size());
            receiver= clients.get(clientIndex);
            if (!receiver.equals(this.getDeXTTAddress())) {
                found = true;
            }
        }

        Date startTime = this.getEarliestBlockTime(); // failsafe, otherwise inconsistencies possible (if TX gets into block with t < t0)
        Date endTime = new Date(Date.from(Instant.now()).getTime() + (1000 * dexxtTransactionTimeSeconds));
        BigInteger amount = BigInteger.valueOf(ThreadLocalRandom.current().nextLong(minimumAmount.longValue(), balance.longValue() + 1)); // random value >= (1 + Witness reward)

        ProofOfIntentData poiData = new ProofOfIntentData(this.getDeXTTAddress(), receiver, amount, startTime, endTime);
        Sign.SignatureData sigA = Cryptography.signPoi(poiData, this.getKeyPair());
        ProofOfIntentRMI poi = new ProofOfIntentRMI(SUPPORTED_VERSION, this.getDeXTTAddress(), receiver, amount, startTime, endTime, sigA);
        return poi;
    }
}
