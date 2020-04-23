package DeXTT;

import Configuration.Configuration;
import Configuration.ContestMode;
import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.DataStructure.ProofOfIntentFull;
import DeXTT.Transaction.ClaimTransaction;
import DeXTT.Transaction.ContestTransaction;
import Events.GlobalEventBus;
import Events.NewBlockFoundEvent;
import Runners.Evaluator;
import com.google.common.eventbus.Subscribe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wf.bitcoin.javabitcoindrpcclient.GenericRpcException;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContestParticipationTask implements Runnable {

    private static final Logger logger = LogManager.getLogger();
    private Evaluator evaluator;

    private BigInteger poiHash;
    private ProofOfIntentFull poiFull;

    private ContestManager contestManager;
    private DeXTTAddress clientAddress;
    private ClientsService clientsService;
    private List<Client> clients;

    private GlobalEventBus globalEventBus;

    private boolean processUnconfirmed;
    private ContestMode contestMode;

    private Map<String, Integer> waitedBlocks;
    private long stopAtSystemMillis;
    private int numberBlocksToWait;

    public ContestParticipationTask(BigInteger poiHash, ProofOfIntentFull poiFull, ContestManager contestManager, DeXTTAddress clientAddress, ClientsService clientsService, long stopAtSystemMillis, int numberBlocksToWait) {
        this.poiHash = poiHash;
        this.poiFull = poiFull;
        this.contestManager = contestManager;
        this.clientAddress = clientAddress;
        this.clientsService = clientsService;
        this.clients = this.clientsService.getClients();
        this.processUnconfirmed = Configuration.getInstance().processUnconfirmedTransactions();
        this.contestMode = Configuration.getInstance().getContestMode();
        this.stopAtSystemMillis = stopAtSystemMillis;
        this.numberBlocksToWait = numberBlocksToWait;
        this.evaluator = Evaluator.getInstance();
        if (!this.processUnconfirmed) {
            this.waitedBlocks = new HashMap<>();
            for (Client client: this.clients) {
                this.waitedBlocks.put(client.getUrlRPC(), 0);
            }
            this.globalEventBus = GlobalEventBus.getInstance();
            this.globalEventBus.getEventBus().register(this);
        }
    }

    public ContestParticipationTask(BigInteger poiHash, ProofOfIntentFull poiFull, ContestManager contestManager, DeXTTAddress clientAddress, ClientsService clientsService) {
        this(poiHash, poiFull, contestManager, clientAddress, clientsService, -1, -1);
    }

    @Subscribe
    public synchronized void newBlockEvent(NewBlockFoundEvent event) {
        // blocktime of new block must be > t0 => otherwise, it does not contain a valid contest
        if (event.getBlockTime().after(this.poiFull.getPoiData().getStartTime())) {
            this.waitedBlocks.compute(event.getRpcUrl(), (k, v) -> v == null ? 1 : v + 1); // count new blocks per blockchain
        }
    }

    /**
     * Sends contest participation, if there is still a chance to win
     */
    @Override
    public void run() {
        try {
            this.participate();
        } catch (Exception e) {
            logger.fatal("Uncaught Exception terminated contest participation: " + e.getMessage());
        } catch (Error e) {
            logger.fatal("Uncaught Error terminated contest participation: " + e.getMessage());
        }
    }

    public void participate() {
        if (!this.processUnconfirmed) {
            // wait for certain number of blocks... or max-timeout
            boolean blockCountReached = false;
            while (!blockCountReached && System.currentTimeMillis() < this.stopAtSystemMillis) {
                // check if blockcount reached
                synchronized (this) {
                    for (Map.Entry<String, Integer> entry : this.waitedBlocks.entrySet()) {
                        if (entry.getValue() >= this.numberBlocksToWait) {
                            // done with waiting
                            blockCountReached = true;
                        }
                    }
                }

                if (!blockCountReached) {
                    try {
                        Thread.sleep(5000); // wait 5 seconds
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
            this.globalEventBus.getEventBus().unregister(this);
        }

        if (this.clientsService.isAfterBlockChainTime(this.poiFull.getPoiData().getEndTime()) // check if latest chaintime is still < t1 (TX endtime)
                && Date.from(Instant.now()).before(this.poiFull.getPoiData().getEndTime())) {  // check if current local time < t1
            BigInteger bestSoFarParticipation = this.contestManager.getBestContestParticipation(this.poiHash);
            BigInteger clientSignature = Cryptography.contestantSignature(clientAddress, this.poiHash);

            if (bestSoFarParticipation.compareTo(clientSignature) < 0) {
                // better signature -> participate
                logger.info("Participating in Contest: " + this.poiHash);
                ClaimTransaction claimTransaction = null;
                ContestTransaction contestTransaction = null;
                // check each client: already has PoiData? -> decide if Full Claim or only claim hash reference needs to be sent
                ContestMode sentMode = null;
                for (Client client : this.clients) {
                    sentMode = null;
                    if (this.contestMode == ContestMode.HASHREFERENCE && client.hasContestStarted(this.poiHash)) {
                        // send contest (hash reference)
                        if (contestTransaction == null) {
                            contestTransaction = new ContestTransaction(this.poiFull.getPoiData());
                        }
                        try {
                            client.sendDeXTTTransaction(contestTransaction);
                            sentMode = ContestMode.HASHREFERENCE;
                        } catch (GenericRpcException e) {
                            logger.fatal("Could not send participation, blockchain communication error: " + e.getMessage());
                        }
                    } else {
                        // send full claim
                        if (claimTransaction == null) {
                            claimTransaction = new ClaimTransaction(this.poiFull.getPoiData(), this.poiFull.getSigA(), this.poiFull.getSigB(), Cryptography.calculateShortPoiHash(this.poiFull.getPoiData()));
                        }
                        try {
                            client.sendDeXTTTransaction(claimTransaction);
                            sentMode = ContestMode.FULL;
                        } catch (GenericRpcException e) {
                            logger.fatal("Could not send participation, blockchain communication error: " + e.getMessage());
                        }
                    }
                    if (sentMode != null) {
                        this.evaluator.addContestParticipation(sentMode);
                    }
                }
            } else {
                logger.info("Not Participating in Contest: " + this.poiHash + ", no chance to win!");
                this.evaluator.addContestNoChanceToWin();
            }
        } else {
            logger.info("Too late to participate.");
            this.evaluator.addContestTooLate();
        }
    }
}
