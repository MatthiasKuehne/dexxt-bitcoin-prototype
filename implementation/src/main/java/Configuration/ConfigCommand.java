package Configuration;
import DeXTT.DataStructure.DeXTTAddress;
import Runners.EvaluationRunner;
import Runners.SystemTestRunner;
import Runners.BlockGenerateRunner;
import Runners.MintRunner;
import DeXTT.Helper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import wf.bitcoin.javabitcoindrpcclient.GenericRpcException;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "DeXTT-Bitcoin", version = "1.0", synopsisSubcommandLabel = "COMMAND")
public class ConfigCommand implements Callable<Integer> {

    private static final Logger logger = LogManager.getLogger();

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    public boolean help = false;

    @CommandLine.Option(names = {"-u", "--urlrpc"}, description = "URLs for the Bitcoin Core RPC API.", required = true, split = ",")
    public List<String> urlRPC;

    @CommandLine.Option(names = {"-a", "--address"}, description = "Bitcoin Address to use.", required = true)
    public String address;

    @CommandLine.Option(names = {"-w", "--walletpassphrase"}, description = "Passphrase for the Bitcoin Core wallet.")
    public String walletPassphrase = null;

    @CommandLine.Option(names = {"-c", "--chain"}, description = "The Bitcoin chain. Valid values: ${COMPLETION-CANDIDATES}", required = true)
    public Chain chain;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    public int genesisBlockHeight;

    public boolean processUnconfirmedTransactions;

    public ContestMode contestMode;

    public List<DeXTTAddress> clientAddresses;

    public boolean allowDoubleSpend;

    public boolean enableAutoUnlocking;

    public int maxContestBlocksWait;
    public int maxContestWaitMilliseconds;

    @Override
    public Integer call() {
        // called if no subcommand is specified
        throw new CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand 'COMMAND'");
    }

    @CommandLine.Command(name="mint", description = "Mints DeXTT tokens.")
    public int mint(@CommandLine.Option(names = {"-m", "--mintAddresses"}, description = "Receiver Addresses of minted tokens", required = true, split = ",", paramLabel = "<addresses>") List<String> addresses,
                    @CommandLine.Option(names = {"-a", "--amount"}, description = "Amount of tokens to be minted", required = true, paramLabel = "<amount>") BigInteger amount,
                    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.") boolean help) {
        for (String address: addresses) {
            logger.debug("Minting: To = " + address + ", amount = " + amount);
        }
        Configuration.init(this);
        MintRunner mintRunner = new MintRunner();
        int ret = 0;
        try {
            ret = mintRunner.execute(addresses, amount);
        } catch (MalformedURLException e) {
            logger.fatal("URL for RPC client malformed: " + e.getMessage());
            return -1;
        } catch (GenericRpcException e) {
            logger.fatal("Not able to initialize, blockchain communication error: " + e.getMessage());
            return -1;
        }
        return ret;
    }

    @CommandLine.Command(name="generateblocks", description = "Generates blocks at given interval")
    public int generateBlocks(@CommandLine.Option(names = {"-r", "--runtime"}, description = "Defines the total runtime for the block generation in seconds.", required = true, paramLabel = "<totalRuntimeSeconds>") long totalRuntimeSeconds,
                              @CommandLine.Option(names = {"-i", "--blockInterval"}, description = "Defines the time interval between two blocks in seconds.", required = true, paramLabel = "<blockTimeIntervalSeconds>") long blockTimeIntervalSeconds,
                              @CommandLine.Option(names = {"-o", "--chainOffset"}, description = "Defines the time offset of block generation between blockchains in seconds.", required = true, paramLabel = "<chainTimeOffsetSeconds>") long chainTimeOffsetSeconds,
                              @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.") boolean help) {
        if (totalRuntimeSeconds > 0 && blockTimeIntervalSeconds > 0 && chainTimeOffsetSeconds >= 0) {
            Configuration.init(this);
            try {
                BlockGenerateRunner blockGenerateRunner = new BlockGenerateRunner(totalRuntimeSeconds, blockTimeIntervalSeconds, chainTimeOffsetSeconds);
                blockGenerateRunner.execute();
            } catch (MalformedURLException e) {
                logger.fatal("URL for RPC client malformed: " + e.getMessage());
                return -1;
            }

        } else {
            logger.fatal("Times must be positive!");
            return -1;
        }
        return 0;
    }

    @CommandLine.Command(name="evaluationrun", description = "Runs evaluation.")
    public int evaluationRun(@CommandLine.Option(names = {"-g", "--genesisBlock"}, description = "The height of the DeXTT genesis block, same for all specified blockchains.", required = true, paramLabel = "<genesisBlockHeight>") int genesisBlockHeight,
                             @CommandLine.Option(names = {"-u", "--processUnconfirmed"}, description = "Processes unconfirmed Transactions, which are not yet in a block.", paramLabel = "<processUnconfirmedTransactions>") boolean processUnconfirmedTransactions,
                             @CommandLine.Option(names = {"-m", "--contestMode"}, description = "Mode for contest transactions: ${COMPLETION-CANDIDATES}", required = true, paramLabel = "<contestMode>") ContestMode contestMode,
                             @CommandLine.Option(names = {"-c", "--clientAddresses"}, description = "DeXTT Addresses of all clients taking part in experiment run.", required = true, split = ",", paramLabel = "<clientAddresses>") List<String> clientAddresses,
                             @CommandLine.Option(names = {"--systemTest"}, description = "Flag that indicates, that a special system test is run.", paramLabel = "<systemTest>") boolean systemTest,
                             @CommandLine.Option(names = {"--allowDoubleSpend"}, description = "Flag that indicates, if double spending transactions received via RMI are sent to the network, used for testing.", paramLabel = "<allowDoubleSpend>") boolean allowDoubleSpend,
                             @CommandLine.Option(names = {"--enableAutoUnlocking"}, description = "Automatically unlocks and re-enables addresses after their veto-finalize, used for testing. Also tries to automatically mints new tokens to re-enabled addresses.", paramLabel = "<enableAutoUnlocking>") boolean enableAutoUnlocking,
                             @CommandLine.Option(names = {"-r", "--runtime"}, description = "Defines the total runtime for the evaluation run.", required = true, paramLabel = "<totalRuntimeSeconds>") long totalRuntimeSeconds,
                             @CommandLine.Option(names = {"-t", "--transactionTime"}, description = "Defines the time span for DeXTT transactions in seconds. (t1 - t0)", required = true, paramLabel = "<dexxtTransactionTimeSeconds>") long dexxtTransactionTimeSeconds,
                             @CommandLine.Option(names = {"-b", "--contestBlocksWait"}, defaultValue = "2", description = "Maximum blocks to wait until contest participations, default = ${DEFAULT-VALUE}. Only taken into account if used WITHOUT <--processUnconfirmed>!", paramLabel = "<maxContestBlocksWait>") int maxContestBlocksWait,
                             @CommandLine.Option(names = {"-w", "--contestTimeWait"}, defaultValue = "10000", description = "Maximum time (Milliseconds) to wait until contest participations, default = ${DEFAULT-VALUE}. Only taken into account if used together WITH <--processUnconfirmed>!", paramLabel = "<maxContestWaitMilliseconds>") int maxContestWaitMilliseconds,
                             @CommandLine.Option(names = {"-f", "--forceVetos"}, description = "Forces Veto Transactions by sending multiple Claims simultaneously", paramLabel = "<forceVetoTransactions>") boolean forceVetoTransactions,
                             @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.") boolean help) {
        this.genesisBlockHeight = genesisBlockHeight;
        this.processUnconfirmedTransactions = processUnconfirmedTransactions;
        this.contestMode = contestMode;
        this.allowDoubleSpend = allowDoubleSpend;
        this.enableAutoUnlocking = enableAutoUnlocking;
        this.maxContestBlocksWait = maxContestBlocksWait;
        this.maxContestWaitMilliseconds = maxContestWaitMilliseconds;
        List<DeXTTAddress> clients = new ArrayList<>();
        for (String s: clientAddresses) {
            if (Helper.isHexString(s) && s.length() == 40) {
                DeXTTAddress c = new DeXTTAddress(s);
                clients.add(c);
            }
        }
        if (clients.size() == 0) {
            // no address in correct format, exit
            logger.error("No client address in correct format (40 character hex string) found!");
            return -1;
        }
        this.clientAddresses = clients;

        Configuration.init(this);
        if (!systemTest) {
            logger.debug("Starting experiment run.");
            EvaluationRunner evaluationRunner = null;
            try {
                evaluationRunner = new EvaluationRunner(totalRuntimeSeconds, dexxtTransactionTimeSeconds, forceVetoTransactions);
            } catch (AlreadyBoundException | RemoteException e) {
                logger.fatal("RMI failed: " + e.getMessage());
                return -1;
            } catch (GenericRpcException e) {
                logger.fatal("Not able to initialize, blockchain communication error: " + e.getMessage());
                return -1;
            } catch (MalformedURLException e) {
                logger.fatal("URL for RPC client malformed: " + e.getMessage());
                return -1;
            }
            evaluationRunner.execute();
        } else {
            logger.debug("Starting system testing...");
            SystemTestRunner systemTestRunner = new SystemTestRunner(dexxtTransactionTimeSeconds, forceVetoTransactions);
            systemTestRunner.execute();
        }
        return 0;
    }
}
