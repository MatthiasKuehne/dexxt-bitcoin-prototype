package DeXTT.Transaction.Bitcoin;

import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.Helper;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.util.Date;

import static Configuration.Constants.DEXTT_KEYWORD_BYTES;

public abstract class BitcoinClaimTransaction implements BitcoinTransaction {

    private Date txTime;

    private int confirmations;

    private BigInteger poiHashShort;

    private DeXTTAddress bitcoinTransactionSender;

    public BitcoinClaimTransaction(Date txTime, int confirmations, BigInteger poiHashShort, DeXTTAddress bitcoinTransactionSender) {
        this.txTime = txTime;
        this.confirmations = confirmations;
        this.poiHashShort = poiHashShort;
        this.bitcoinTransactionSender = bitcoinTransactionSender;
    }

    @Override
    public BigInteger getPoiHashShort() {
        return this.poiHashShort;
    }

    public Date getTxTime() {
        return txTime;
    }

    public int getConfirmations() {
        return confirmations;
    }

    public DeXTTAddress getBitcoinTransactionSender() {
        return bitcoinTransactionSender;
    }

    protected byte[] convertToDeXTTPayload(int transactionLength, int transactionType, Sign.SignatureData sig) {
        byte[] payload = new byte[(DEXTT_KEYWORD_BYTES.length + transactionLength)];

        Helper.putDeXTTKeywordAndVersionToPayload(payload);
        payload[DEXTT_KEYWORD_BYTES.length + 1] = (byte) transactionType;

        int startIndex = DEXTT_KEYWORD_BYTES.length + 2;
        Helper.putSignatureIntoPayload(payload, sig, startIndex);
        startIndex += sig.getV().length + sig.getR().length + sig.getS().length;
        Helper.putAmountIntoPayload(payload, this.getPoiHashShort(), startIndex, 8);

        return payload;
    }
}
