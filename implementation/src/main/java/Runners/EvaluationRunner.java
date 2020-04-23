package Runners;

import Communication.RMI.PoIMessengerInterface;
import Communication.RMI.ProofOfIntentRMI;
import Communication.RMI.RMIProvider;
import Configuration.Configuration;
import DeXTT.Client;
import DeXTT.DataStructure.*;
import DeXTT.ClientsService;
import DeXTT.Wallet;
import Events.GlobalEventBus;
import Events.NewBlockFoundEvent;
import DeXTT.Cryptography;
import DeXTT.Helper;
import com.google.common.eventbus.Subscribe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wf.bitcoin.javabitcoindrpcclient.GenericRpcException;

import java.math.BigInteger;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class EvaluationRunner {

    private static final Logger logger = LogManager.getLogger();
    private Configuration configuration;
    private ClientsService clientsService;
    private GlobalEventBus globalEventBus;
    private Evaluator evaluator;

    private long totalRuntimeSeconds;
    private long dexxtTransactionTimeSeconds;
    private boolean forceVetoTransactions;

    private long minimumPoiWaitSeconds = 692; // 15s equivalent for 10m block times
    private long maximumPoiWaitSeconds = 1385; // 30s equivalent for 10m block times

    private PoITimeHash endTime = null;
    private VetoFinalizeData vetoEndTime = null;

    // cache lock status for polling, only overwrite on new block event
    private boolean isSenderUnlocked = false;
    private boolean allSendersUnlocked = false;

    private long nextPoiAllowedTime = 0;

    public EvaluationRunner(long totalRuntimeSeconds, long dexxtTransactionTimeSeconds, boolean forceVetoTransactions) throws AlreadyBoundException, RemoteException, GenericRpcException, MalformedURLException {
        this.configuration = Configuration.getInstance();
        this.globalEventBus = GlobalEventBus.getInstance();
        this.evaluator = Evaluator.getInstance();
        this.clientsService = new ClientsService();
        this.totalRuntimeSeconds = totalRuntimeSeconds;
        this.dexxtTransactionTimeSeconds = dexxtTransactionTimeSeconds;
        this.forceVetoTransactions = forceVetoTransactions;
        this.clientsService.initialize();
        this.isSenderUnlocked = this.senderIsUnlocked();
        this.globalEventBus.getEventBus().register(this);
    }

    public void execute() {
        // wait until all clients are up (RMI)!!
        boolean clientsRunning = false;
        boolean printedMessage = false;
        while(!clientsRunning) {
            try {
                for (DeXTTAddress address : this.configuration.getClientAddresses()) {
                    if (!address.equals(this.clientsService.getDeXTTAddress())) {
                        PoIMessengerInterface poIMessengerInterface = RMIProvider.getRemoteObjectStub(address);
                    }
                }
                clientsRunning = true;
                logger.info("RMI of other Clients running.");
            } catch (RemoteException | NotBoundException e) {
                clientsRunning = false;
                if (!printedMessage) {
                    logger.info("Waiting for startup of RMI of other Clients...");
                    printedMessage = true;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    this.clientsService.close();
                    e.printStackTrace();
                    return;
                }
            }
        }

        Map<DeXTTAddress, BigInteger> balanceCache = new HashMap<>();
        for (DeXTTAddress address: this.configuration.getClientAddresses()) {
            balanceCache.put(address, BigInteger.valueOf(-1));
        }

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (1000L * totalRuntimeSeconds);
        while (System.currentTimeMillis() < endTime || !this.isSenderUnlocked || !this.allSendersUnlocked) { // respect runtime + wait until all Transactions are done
            this.clientsService.loopIteration(); // do one iteration (new blocks/txs/RMI)

            // print changed balances (newest value -> latest blockchain)
            for (DeXTTAddress address: this.configuration.getClientAddresses()) {
                BigInteger newBalance = this.clientsService.getBalance(address);
                if (!newBalance.equals(balanceCache.get(address))) {
                    // new balance:
                    logger.info("New Balance for " + address + ": " + newBalance);
                    balanceCache.put(address, newBalance);
                }
            }

            if ((endTime - System.currentTimeMillis()) > (this.dexxtTransactionTimeSeconds * 1000L)
                    && System.currentTimeMillis() > this.nextPoiAllowedTime) { // enough time left
                this.tryToSendPois();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                this.clientsService.close();
                e.printStackTrace();
                break;
            }
        }

        this.evaluator.printEvaluation();
        this.clientsService.close();
        logger.info("Evaluation done, runtime limit reached.");
    }

    private void tryToSendPois() {
        if (this.endTime == null && this.vetoEndTime == null && this.isSenderUnlocked) {
            // try to send PoI
            int numClaims = 1;
            if (forceVetoTransactions) {
                numClaims++;
            }
            List<ProofOfIntentRMI> pois = new ArrayList<>();
            for (int i = 0; i < numClaims; i++) {
                ProofOfIntentRMI poi = this.clientsService.createRandomPoi(this.dexxtTransactionTimeSeconds);
                if (poi != null) {
                    try {
                        PoIMessengerInterface poIMessengerInterface = RMIProvider.getRemoteObjectStub(poi.getReceiver());
                        poIMessengerInterface.registerPoI(poi);
                        pois.add(poi);
                        logger.info("Sent PoI to: " + poi.getReceiver());
                    } catch (RemoteException | NotBoundException e) {
                        logger.error("RMI not running, could not send PoI!");
                    }
                } else {
                    logger.info("Could not create new PoI!");
                }
            }

            if (pois.size() == 2) {
                // save veto endtime for later checks
                Date vetoEndTime = Helper.calculateVetoEndTime(new ProofOfIntentData(pois.get(0)), new ProofOfIntentData(pois.get(1)));
                this.vetoEndTime = new VetoFinalizeData(vetoEndTime, this.clientsService.getDeXTTAddress());
                this.endTime = null;
            } else if (pois.size() == 1) {
                // save poi endtime for later checks
                ProofOfIntentRMI poi = pois.get(0);
                BigInteger poiHash = Cryptography.calculateFullPoiHash(new ProofOfIntentData(poi.getSender(), poi.getReceiver(), poi.getAmount(), poi.getStartTime(), poi.getEndTime()));
                this.endTime = new PoITimeHash(poi.getEndTime(), poiHash);
                this.vetoEndTime = null;
            }
        }
    }

    @Subscribe
    public void newBlockFoundEvent(NewBlockFoundEvent event) {
        this.isSenderUnlocked = this.senderIsUnlocked();
        this.allSendersUnlocked = this.allSendersUnlocked();
        if (this.endTime != null && this.clientsService.getEarliestBlockTime().after(this.endTime.getEndTime()) && this.isSenderUnlocked) {
            // poi has ended and all wallets have unlocked the sender: Either PoI was never there (too late) or already finalized
            // check wallet states (was processed, same winner on all chains)
            boolean transactionSuccess = true;
            boolean wasNotStarted = false;
            DeXTTAddress winner = null;
            for (Client client : this.clientsService.getClients()) {
                Wallet wallet = client.getWallet();
                DeXTTAddress otherWinner = wallet.getContestWinner(this.endTime.getPoiHash());
                if (winner == null) {
                    winner = otherWinner;
                }
                if (!wallet.isOngoingPoi(this.endTime.getPoiHash()) || otherWinner == null) {
                    transactionSuccess = false;
                    wasNotStarted = true;
                } else if (!otherWinner.equals(winner)) {
                    // not started or not same winner
                    transactionSuccess = false;
                }
            }

            this.evaluator.addTransactionCount(transactionSuccess, wasNotStarted);

            // done, clear entry -> possible to send new Poi
            this.endTime = null;
            this.setNewPoiWaitTime();

        } else if (this.vetoEndTime != null && this.clientsService.getEarliestBlockTime().after(this.vetoEndTime.getEndTime()) && this.isSenderUnlocked) {
            // veto has ended and all wallets have unlocked the sender (only works with --enableAutoUnlocking): Either PoI/veto was never there (too late) or already veto-finalized
            // All Veto fields in Wallet are reset, except Veto-Winner
            boolean transactionSuccess = true;
            boolean wasVeto = true;
            DeXTTAddress winner = null;
            for (Client client: this.clientsService.getClients()) {
                Wallet wallet = client.getWallet();
                DeXTTAddress otherWinner = wallet.getVetoContestWinner(this.vetoEndTime.getConflictingPoiSender());
                if (winner == null) {
                    winner = otherWinner;
                }
                if (otherWinner == null) {
                    transactionSuccess = false;
                    wasVeto = false; // no entry found, was no veto contest
                } else if (!otherWinner.equals(winner)) {
                    transactionSuccess = false;
                }

                wallet.deleteVetoWinnerEntry(this.vetoEndTime.getConflictingPoiSender());
            }
            this.evaluator.addVetoTransactionCount(transactionSuccess, wasVeto);

            // done, clear entry -> possible to send new Poi
            this.vetoEndTime = null;
            this.setNewPoiWaitTime();
        }
    }

    private boolean senderIsUnlocked() {
        for (Client client: this.clientsService.getClients()) {
            Wallet wallet = client.getWallet();
            if (wallet.lockStatus(this.clientsService.getDeXTTAddress()) != null) {
                return false;
            }
        }
        return true;
    }

    private boolean allSendersUnlocked() {
        for (Client client: this.clientsService.getClients()) {
            Wallet wallet = client.getWallet();
            for (DeXTTAddress addr: this.configuration.getClientAddresses()) {
                if (wallet.lockStatus(addr) != null) {
                    return false;
                }
            }
        }
        return true;
    }

    private void setNewPoiWaitTime() {
        long waitTime = ThreadLocalRandom.current().nextLong(this.minimumPoiWaitSeconds, this.maximumPoiWaitSeconds);
        this.nextPoiAllowedTime = System.currentTimeMillis() + (waitTime * 1000L);
        logger.info("Waiting for " + waitTime + " seconds for next PoI.");
    }
}
