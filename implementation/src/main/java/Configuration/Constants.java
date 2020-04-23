package Configuration;

import DeXTT.DataStructure.DeXTTAddress;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class Constants {

    public static final String DEXTT_KEYWORD = "DeXTT";

    public static final byte[] DEXTT_KEYWORD_BYTES = DEXTT_KEYWORD.getBytes(StandardCharsets.UTF_8);

    public static final int SUPPORTED_VERSION = 0x01;

    public static final BigInteger WITNESS_REWARD = BigInteger.valueOf(1);

    public static final BigDecimal DEFAULT_FEE = new BigDecimal("0.0002"); // default fallback: 0.0002 BTC/kB

    public static final BigDecimal MINIMUM_FEE = new BigDecimal("0.00001"); // minimum fee according to "minrelaytxfee" and "mempoolminfee" of blockchain

    //    private BigDecimal sizeEstimateMultiplier = new BigDecimal("0.145"); // 145 vbytes with empty OP_RETURN payload for segwit in p2sh
    public static final BigDecimal SIZE_ESTIMATE_MULTIPLIER = new BigDecimal("0.121"); // 121 vbytes with empty OP_RETURN payload for native segwit

    public static final DeXTTAddress MINTING_ADDRESS = new DeXTTAddress("caae72556fc36d3a8c859029047119b712717419");

    public static final int RMI_PORT = 1099;

    /**
     * percentage of TX duration, which is the maximum to wait until contest participation
     */
    public static final double maxContestWaitPercentage = 0.5;

    /**
     * Transaction types
     */

    public static final int CLAIM_DATA_TRANSACTION_TYPE = 0x01;

    public static final int CLAIM_SIG_TRANSACTION_A_TYPE = 0x02;

    public static final int CLAIM_SIG_TRANSACTION_B_TYPE = 0x03;

    public static final int CONTEST_PARTICIPATION_TRANSACTION_TYPE = 0x04;

    public static final int FINALIZE_TRANSACTION_TYPE = 0x05;

    public static final int FINALIZE_VETO_TRANSACTION_TYPE = 0x06;

    public static final int MINT_TRANSACTION_TYPE = 0xFF;

    /**
     * Transaction lengths without deXTTKeyword prefix, in bytes
     */

    public static final int CLAIM_DATA_TRANSACTION_LENGTH = 74;

    public static final int CLAIM_SIG_TRANSACTION_A_LENGTH = 75;

    public static final int CLAIM_SIG_TRANSACTION_B_LENGTH = 75;

    public static final int CONTEST_PARTICIPATION_TRANSACTION_LENGTH = 34;

    public static final int FINALIZE_TRANSACTION_LENGTH = 34;

    public static final int FINALIZE_VETO_TRANSACTION_LENGTH = 22;

    public static final int MINT_TRANSACTION_LENGTH = 38;
}
