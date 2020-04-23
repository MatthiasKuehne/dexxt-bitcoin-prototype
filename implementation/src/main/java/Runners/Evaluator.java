package Runners;

import Configuration.Configuration;
import Configuration.ContestMode;
import DeXTT.Transaction.*;
import com.google.common.math.Stats;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

// Singleton
public class Evaluator {

    private static Evaluator instance;
    private static final Logger logger = LogManager.getLogger();
    private Configuration configuration;

    List<String> transactionNamesToEvaluateCosts;
    private Map<String, List<Long>> transactionsSizes; // key: transaction name, value: list of all measured transaction sizes in vbytes

    // only contest participation where this is not the receiver (receiver automatically is contest participant)
    private long totalContestParticipationCount = 0;
    private long contestsParticipated = 0;
    private long contestsParticipatedFull = 0;
    private long contestsParticipatedHashReference = 0;
    private long contestsNoChanceToWin = 0;
    private long contestsTooLate = 0;

    private int sentPois = 0;
    private int successfulTransactions = 0; // also counts vetos if they are intended
    private int failedTransactions = 0;
    private int vetoTransactions = 0;
    private int failCauseNotStarted = 0;
    private int failCauseDifferentWinner = 0;

    private Evaluator() {
        this.configuration = Configuration.getInstance();
        this.transactionsSizes = new HashMap<>();
        transactionNamesToEvaluateCosts = Arrays.asList(ClaimTransaction.class.getSimpleName(), ContestTransaction.class.getSimpleName(), FinalizeTransaction.class.getSimpleName(), FinalizeVetoTransaction.class.getSimpleName());

        // print csv headers (is duplicated by multiple clients)
        logger.log(Level.getLevel("TRANSACTION_EVALUATION"), "client BTC Address,total TXs sent,successful TXs,failed TXs,veto TXs,failed not started,failed different winner,percentage successful,percentage fail not started,percentage fail different winner");

        StringBuilder costHeader = new StringBuilder();
        costHeader.append("client BTC Address");
        for (String txName: transactionNamesToEvaluateCosts) {
            costHeader.append(",").append(txName).append(" avg,").append(txName).append(" deviation,").append(txName).append(" count");
        }
        logger.log(Level.getLevel("COST_EVALUATION"), costHeader); // "claim avg,claim deviation,contest avg,contest deviation,finalize avg,finalize deviation,finalize-veto avg,finalize-veto deviation"
        logger.log(Level.getLevel("CONTEST_PARTICIPATION_EVALUATION"), "client BTC Address,total contests,participated,participated full, participated hash reference,no chance to win,too late,percentage participated,percentage participated hash reference,percentage no chance to win,percentage too late");
    }

    public static synchronized Evaluator getInstance() {
        if (instance == null) {
            instance = new Evaluator();
        }
        return instance;
    }

    public synchronized void addTransactionSizeEntry(String transactionName, long size) {
        this.transactionsSizes.compute(transactionName, (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            v.add(size);
            return v;
        });
    }

    public synchronized void addContestParticipation(ContestMode mode) {
        this.totalContestParticipationCount++;
        this.contestsParticipated++;
        if (mode == ContestMode.FULL) {
            this.contestsParticipatedFull++;
        } else if (mode == ContestMode.HASHREFERENCE) {
            this.contestsParticipatedHashReference++;
        }
    }

    public synchronized void addContestNoChanceToWin() {
        this.totalContestParticipationCount++;
        this.contestsNoChanceToWin++;
    }

    public synchronized void addContestTooLate() {
        this.totalContestParticipationCount++;
        this.contestsTooLate++;
    }

    public void addTransactionCount(boolean success, boolean wasNotStarted) {
        this.addTransactionCount(success, false, false, wasNotStarted);
    }

    public void addVetoTransactionCount(boolean success, boolean wasVeto) {
        this.addTransactionCount(success, true, wasVeto, false);
    }

    private void addTransactionCount(boolean success, boolean shouldBeVeto, boolean wasVeto, boolean wasNotStarted) {
        int addend = 1;
        if (shouldBeVeto) {
            addend = 2;
        }
        this.sentPois += addend;
        if (success) {
            this.successfulTransactions += addend;
            if (shouldBeVeto) {
                this.vetoTransactions += addend;
            }
        } else {
            this.failedTransactions += addend;
            if (wasVeto) {
                this.vetoTransactions += addend;
                if (shouldBeVeto) {
                    this.failCauseDifferentWinner += addend; // only possibility if failed + was Veto + should be veto
                }
            }
            if (!shouldBeVeto) {
                // only count cause for non-Veto
                if (wasNotStarted) {
                    this.failCauseNotStarted += addend;
                } else {
                    this.failCauseDifferentWinner += addend;
                }
            }
        }
        logger.debug("Transaction was: successful = " + success + (shouldBeVeto ? "" : ", wasNotStarted = " + wasNotStarted));
        logger.info("Current stats: Sent PoIs = " + this.sentPois + ", successful = " + this.successfulTransactions + ", failed = " + this.failedTransactions
                + ", vetos = " + this.vetoTransactions + ", failed not started = " + this.failCauseNotStarted + ", failed different winner = " + this.failCauseDifferentWinner);
    }

    /**
     * prints evaluation via logger, in csv format
     */
    public synchronized void printEvaluation() {
        // evaluate transaction success
        double percentageSuccessful = -1;
        double percentageFailedNotStarted = -1;
        double percentageFailedDifferentWinner = -1;
        if (sentPois > 0) {
            percentageSuccessful = 100 * (double) successfulTransactions / (double) sentPois;
        }
        if (failedTransactions > 0) {
            percentageFailedNotStarted = 100 * (double) failCauseNotStarted / (double) failedTransactions;
            percentageFailedDifferentWinner = 100 * (double) failCauseDifferentWinner / (double) failedTransactions;
        }

        logger.log(Level.getLevel("TRANSACTION_EVALUATION"), this.configuration.getBitcoinAddress() + "," + sentPois + "," + successfulTransactions + "," + failedTransactions + ","
                + vetoTransactions + "," + failCauseNotStarted + "," + failCauseDifferentWinner + "," + percentageSuccessful + "," + percentageFailedNotStarted + "," + percentageFailedDifferentWinner);


        // evaluate costs
        StringBuilder costEvaluation = new StringBuilder();
        costEvaluation.append(this.configuration.getBitcoinAddress());
        for (String txName: this.transactionNamesToEvaluateCosts) {
            costEvaluation.append(",");
            List<Long> sizes = this.transactionsSizes.get(txName);
            if (sizes != null && sizes.size() > 0) {
                Stats stats = Stats.of(sizes);
                double avg = stats.mean();
                double standardDeviation = stats.populationStandardDeviation();
                costEvaluation.append(avg).append(",").append(standardDeviation).append(",").append(sizes.size());
            } else {
                costEvaluation.append("undef,undef,0");
            }
        }
        logger.log(Level.getLevel("COST_EVALUATION"), costEvaluation);

        // evaluate contest participation
        double percentageContestsParticipated = -1;
        double percentageContestsParticipatedHashReference = -1;
        double percentageContestsNoChanceToWin = -1;
        double percentageContestsTooLate = -1;
        if (totalContestParticipationCount > 0) {
            percentageContestsParticipated = 100 * (double) contestsParticipated / (double) totalContestParticipationCount;
            percentageContestsNoChanceToWin = 100 * (double) contestsNoChanceToWin / (double) totalContestParticipationCount;
            percentageContestsTooLate = 100 * (double) contestsTooLate / (double) totalContestParticipationCount;
        }
        if (contestsParticipated > 0) {
            percentageContestsParticipatedHashReference = 100 * (double) contestsParticipatedHashReference / (double) contestsParticipated;
        }

        logger.log(Level.getLevel("CONTEST_PARTICIPATION_EVALUATION"), this.configuration.getBitcoinAddress() + "," + totalContestParticipationCount + "," + contestsParticipated + "," + contestsParticipatedFull + "," + contestsParticipatedHashReference + "," +
                + contestsNoChanceToWin + "," + contestsTooLate + "," + percentageContestsParticipated + "," + percentageContestsParticipatedHashReference + "," + percentageContestsNoChanceToWin + "," + percentageContestsTooLate);
    }
}
