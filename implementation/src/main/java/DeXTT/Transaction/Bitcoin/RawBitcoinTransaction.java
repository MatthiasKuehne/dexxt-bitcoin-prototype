package DeXTT.Transaction.Bitcoin;

import java.util.Date;

public class RawBitcoinTransaction {

    private Date time;

    private String SenderPubKeyHexCompressed;

    private String txId; // txId of Bitcoin transaction

    private int confirmations;

    /**
     * DeXTT payload without "DeXTT" keyword prefix
     */
    private byte[] payload;

    public RawBitcoinTransaction(String pubKeyHexCompressed, byte[] payload, String txId, int confirmations) {
        this.SenderPubKeyHexCompressed = pubKeyHexCompressed;
        this.payload = payload;
        this.txId = txId;
        this.confirmations = confirmations;
        this.time = null;
    }

    public RawBitcoinTransaction(Date time, String pubKeyHexCompressed, byte[] payload, String txId, int confirmations) {
        this(pubKeyHexCompressed, payload, txId, confirmations);
        this.time = time;
    }

    /**
     *
     * @return null if no blocktime available
     *          Block time of TX: If confirmations > 0
     *          Block time of last mined block: if confirmations == 0 || null (unconfirmed tx)
     */
    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getPubKeyHexCompressed() {
        return SenderPubKeyHexCompressed;
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getTxId() {
        return txId;
    }

    public int getConfirmations() {
        return confirmations;
    }
}
