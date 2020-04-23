package Runners;

import Communication.Bitcoin.BitcoinCommunicator;
import Configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wf.bitcoin.javabitcoindrpcclient.GenericRpcException;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BlockGenerateRunner {

    private static final Logger logger = LogManager.getLogger();

    private Configuration configuration;

    private long totalRuntimeSeconds;
    private long blockTimeIntervalSeconds;
    private long chainTimeOffsetSeconds;

    private ScheduledExecutorService executorService;
    private List<BitcoinCommunicator> bitcoinCommunicators;

    public BlockGenerateRunner(long totalRuntimeSeconds, long blockTimeIntervalSeconds, long chainTimeOffsetSeconds) throws MalformedURLException {
        configuration = Configuration.getInstance();
        this.totalRuntimeSeconds = totalRuntimeSeconds;
        this.blockTimeIntervalSeconds = blockTimeIntervalSeconds;
        this.chainTimeOffsetSeconds = chainTimeOffsetSeconds;

        this.bitcoinCommunicators = new ArrayList<>();
        for (String url: this.configuration.getUrlRPC()) {
            this.bitcoinCommunicators.add(new BitcoinCommunicator(url));
        }

        this.executorService = Executors.newScheduledThreadPool(this.bitcoinCommunicators.size());
    }

    public void execute() {
        // setup/start
        int count = 0;
        for (BitcoinCommunicator bc: this.bitcoinCommunicators) {
            this.executorService.scheduleAtFixedRate(() -> {
                try {
                    bc.generateToAddress(this.configuration.getBitcoinAddress());
                    logger.info("[" + bc.getUrl() + "] Generated block to " + this.configuration.getBitcoinAddress());
                } catch (GenericRpcException e) {
                    logger.error("[" + bc.getUrl() + "] Blockchain communication error.");
                }
            }, this.chainTimeOffsetSeconds * count, this.blockTimeIntervalSeconds, TimeUnit.SECONDS);
            count++;
        }

        // wait
        try {
            Thread.sleep(this.totalRuntimeSeconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
}
