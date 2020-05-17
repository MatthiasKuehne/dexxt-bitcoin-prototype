package DeXTT.Transaction.Bitcoin;

import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.Helper;
import DeXTT.Transaction.FinalizeVetoTransaction;
import DeXTT.Transaction.Transaction;

import java.util.Date;

import static Configuration.Constants.*;

public class BitcoinFinalizeVetoTransaction implements BitcoinTransaction {

    private DeXTTAddress conflictingPoiSender;

    private Date txTime;

    public BitcoinFinalizeVetoTransaction(DeXTTAddress conflictingPoiSender, Date txTime) {
        this.conflictingPoiSender = conflictingPoiSender;
        this.txTime = txTime;
    }

    public BitcoinFinalizeVetoTransaction(DeXTTAddress conflictingPoiSender) {
        this.conflictingPoiSender = conflictingPoiSender;
        this.txTime = null;
    }

    @Override
    public Transaction createNewCorrespondingDeXTTTransaction() {
        return new FinalizeVetoTransaction(this.conflictingPoiSender, this.txTime);
    }

    @Override
    public byte[] convertToDeXTTPayload() {
        byte[] payload = new byte[(DEXTT_KEYWORD_BYTES.length + FINALIZE_VETO_TRANSACTION_LENGTH)];

        Helper.putDeXTTKeywordAndVersionToPayload(payload);
        payload[DEXTT_KEYWORD_BYTES.length + 1] = (byte) FINALIZE_VETO_TRANSACTION_TYPE;

        int startIndex = DEXTT_KEYWORD_BYTES.length + 2;
        byte[] poiSender = this.conflictingPoiSender.getAddressBytesWithoutPrefix();
        Helper.putArrayIntoPayload(payload, poiSender, startIndex, startIndex + poiSender.length);

        return payload;
    }

}
