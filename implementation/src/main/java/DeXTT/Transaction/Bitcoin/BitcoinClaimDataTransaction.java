package DeXTT.Transaction.Bitcoin;

import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.Exception.AlreadyAddedTransactionException;
import DeXTT.DataStructure.ProofOfIntentData;
import DeXTT.Transaction.ClaimTransaction;
import DeXTT.Transaction.Transaction;
import DeXTT.Helper;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import static Configuration.Constants.*;

public class BitcoinClaimDataTransaction extends BitcoinClaimTransaction {

    private ProofOfIntentData poi;

    public BitcoinClaimDataTransaction(Date txTime, int confirmations, DeXTTAddress sender, DeXTTAddress receiver, BigInteger amount, Date startTime, Date endTime, BigInteger poiHashShort, DeXTTAddress bitcoinTransactionSender) {
        super(txTime,confirmations, poiHashShort, bitcoinTransactionSender);
        this.poi = new ProofOfIntentData(sender, receiver, amount, startTime, endTime);
    }

    /**
     * Only for use if Tx has to be sent (no date/confirmations available and needed)
     * @param poi
     * @param poiHashShort
     */
    public BitcoinClaimDataTransaction(ProofOfIntentData poi, BigInteger poiHashShort) {
        super(null, -1, poiHashShort, null);
        this.poi = poi;
    }

    @Override
    public Transaction createNewCorrespondingDeXTTTransaction() {
        return new ClaimTransaction(this);
    }

    @Override
    public List<Transaction> tryToAddDeXTTBitcoinTransaction(Transaction transaction) throws AlreadyAddedTransactionException {
        return transaction.tryToAddDeXTTBitcoinTransaction(this);
    }

    @Override
    public byte[] convertToDeXTTPayload() {
        byte[] payload = new byte[(DEXTT_KEYWORD_BYTES.length + CLAIM_DATA_TRANSACTION_LENGTH)];

        Helper.putDeXTTKeywordAndVersionToPayload(payload);
        payload[DEXTT_KEYWORD_BYTES.length + 1] = (byte) CLAIM_DATA_TRANSACTION_TYPE;

        byte[] sender = this.getSender().getAddressBytesWithoutPrefix();
        int startIndex = DEXTT_KEYWORD_BYTES.length + 2;
        int endIndex = startIndex + sender.length;
        Helper.putArrayIntoPayload(payload, sender, startIndex, endIndex);

        byte[] receiver = this.getReceiver().getAddressBytesWithoutPrefix();
        startIndex = endIndex;
        endIndex = startIndex + receiver.length;
        Helper.putArrayIntoPayload(payload, receiver, startIndex, endIndex);

        startIndex = endIndex;
        Helper.putAmountIntoPayload(payload, this.getAmount(), startIndex, 16);
        startIndex += 16;

        // time is stored in seconds (same as in Bitcoin blocks)
        Helper.putDateIntoPayload(payload, this.getStartTime(), startIndex, 4);
        startIndex += 4;
        Helper.putDateIntoPayload(payload, this.getEndTime(), startIndex, 4);
        startIndex += 4;
        Helper.putAmountIntoPayload(payload, this.getPoiHashShort(), startIndex, 8);

        return payload;
    }

    public ProofOfIntentData getPoi() {
        return poi;
    }

    public DeXTTAddress getSender() {
        return this.poi.getSender();
    }

    public DeXTTAddress getReceiver() {
        return this.poi.getReceiver();
    }

    public BigInteger getAmount() {
        return this.poi.getAmount();
    }

    public Date getStartTime() {
        return this.poi.getStartTime();
    }

    public Date getEndTime() {
        return this.poi.getEndTime();
    }

    public boolean representsSamePoI(ProofOfIntentData poi) {
        return this.poi.equals(poi);
    }
}
