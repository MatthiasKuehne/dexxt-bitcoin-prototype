package DeXTT.Transaction.Bitcoin;

import DeXTT.Helper;

import java.math.BigInteger;
import java.util.Date;

import static Configuration.Constants.DEXTT_KEYWORD_BYTES;

public abstract class BitcoinHashReferenceTransaction implements BitcoinTransaction {

    private BigInteger poiHashFull;

    private BigInteger poiHashShort;

    private Date txTime;

    public BitcoinHashReferenceTransaction(BigInteger poiHashFull, BigInteger poiHashShort, Date txTime) {
        this.poiHashFull = poiHashFull;
        this.poiHashShort = poiHashShort;
        this.txTime = txTime;
    }

    @Override
    public BigInteger getPoiHashShort() {
        return this.poiHashShort;
    }

    public BigInteger getPoiHashFull() {
        return poiHashFull;
    }

    public Date getTxTime() {
        return txTime;
    }

    protected byte[] convertToDeXTTPayload(int transactionLength, int transactionType) {
        byte[] payload = new byte[(DEXTT_KEYWORD_BYTES.length + transactionLength)];

        Helper.putDeXTTKeywordAndVersionToPayload(payload);
        payload[DEXTT_KEYWORD_BYTES.length + 1] = (byte) transactionType;

        int startIndex = DEXTT_KEYWORD_BYTES.length + 2;
        Helper.putAmountIntoPayload(payload, this.getPoiHashFull(), startIndex, 32);

        return payload;
    }
}
