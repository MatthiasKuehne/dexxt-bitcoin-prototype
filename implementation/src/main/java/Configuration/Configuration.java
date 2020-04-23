package Configuration;

import DeXTT.DataStructure.DeXTTAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.NetworkParameters;

import java.util.List;

import static java.lang.System.exit;

public class Configuration {
    private static final Logger logger = LogManager.getLogger();

    private static Configuration configuration; // singleton instance

    private String bitcoinAddress;

    // addresses of other running clients for test runs.
    private List<DeXTTAddress> clientAddresses;

    private List<String> urlRPC;

    private String walletPassphrase;

    private NetworkParameters networkParameters;

    private int genesisBlockHeight;

    private boolean processUnconfirmedTransactions;

    private ContestMode contestMode;

    private boolean allowDoubleSpend;

    private boolean enableAutoUnlocking;

    /**
     * maximum number of blocks to wait with contest participation when using only confirmed transactions
     */
    private int maxContestBlocksWait;

    /**
     * maximum wait time of contest participation when using unconfirmed transactions, in milliseconds
     */
    private int maxContestWaitMilliseconds;

    private Configuration(List<String> urlRPC, String bitcoinAddress, String walletPassphrase, Chain chain, int genesisBlockHeight, boolean processUnconfirmedTransactions,
                          ContestMode contestMode, List<DeXTTAddress> clientAddresses, boolean allowDoubleSpend, boolean enableAutoUnlocking,
                          int maxContestBlocksWait, int maxContestWaitMilliseconds) {
        this.urlRPC = urlRPC;
        this.bitcoinAddress = bitcoinAddress;
        this.walletPassphrase = walletPassphrase;
        this.genesisBlockHeight = genesisBlockHeight;
        this.processUnconfirmedTransactions = processUnconfirmedTransactions;
        this.contestMode = contestMode;
        this.clientAddresses = clientAddresses;
        this.allowDoubleSpend = allowDoubleSpend;
        this.enableAutoUnlocking = enableAutoUnlocking;
        this.maxContestBlocksWait = maxContestBlocksWait;
        this.maxContestWaitMilliseconds = maxContestWaitMilliseconds;

        switch (chain) {
            case REGTEST:
                this.networkParameters = NetworkParameters.fromID(NetworkParameters.ID_REGTEST);
                break;
            case TESTNET:
                this.networkParameters = NetworkParameters.fromID(NetworkParameters.ID_TESTNET);
                break;
            case MAINNET:
                this.networkParameters = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);
                break;
            default:
                logger.fatal("No chain specified, should not happen.");
                exit(1);
        }
    }

    public static void init(ConfigCommand config) {
        if (Configuration.configuration == null) {
            Configuration.configuration = new Configuration(config.urlRPC, config.address, config.walletPassphrase, config.chain, config.genesisBlockHeight,
                    config.processUnconfirmedTransactions, config.contestMode, config.clientAddresses, config.allowDoubleSpend, config.enableAutoUnlocking,
                    config.maxContestBlocksWait, config.maxContestWaitMilliseconds);
        }
    }

    public static synchronized Configuration getInstance() {
        if (Configuration.configuration == null) {
            logger.fatal("Configuration.configuration not initialized, exiting.");
            throw new IllegalStateException("Configuration was not initialized before requesting instance!");
        }
        return Configuration.configuration;
    }

    public String getBitcoinAddress() {
        return bitcoinAddress;
    }

    public List<String> getUrlRPC() {
        return urlRPC;
    }

    public String getWalletPassphrase() {
        return walletPassphrase;
    }

    public NetworkParameters getNetworkParameters() {
        return networkParameters;
    }

    public int getGenesisBlockHeight() {
        return genesisBlockHeight;
    }

    public boolean processUnconfirmedTransactions() {
        return processUnconfirmedTransactions;
    }

    public ContestMode getContestMode() {
        return contestMode;
    }

    /**
     * addresses of other running clients for test runs.
     * @return
     */
    public List<DeXTTAddress> getClientAddresses() {
        return clientAddresses;
    }

    public boolean isAllowDoubleSpend() {
        return allowDoubleSpend;
    }

    public boolean isEnableAutoUnlocking() {
        return enableAutoUnlocking;
    }

    /**
     * maximum number of blocks to wait with contest participation when using only confirmed transactions
     * @return
     */
    public int getMaxContestBlocksWait() {
        return maxContestBlocksWait;
    }

    /**
     * maximum wait time of contest participation when using unconfirmed transactions, in milliseconds
     * @return
     */
    public int getMaxContestWaitMilliseconds() {
        return maxContestWaitMilliseconds;
    }
}