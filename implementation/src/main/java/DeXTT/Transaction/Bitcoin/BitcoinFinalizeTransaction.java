package DeXTT.Transaction.Bitcoin;

import DeXTT.Transaction.FinalizeTransaction;
import DeXTT.Transaction.Transaction;

import java.math.BigInteger;
import java.util.Date;

import static Configuration.Constants.*;

public class BitcoinFinalizeTransaction extends BitcoinHashReferenceTransaction {

    public BitcoinFinalizeTransaction(BigInteger poiHashFull, BigInteger poiHashShort, Date txTime) {
        super(poiHashFull, poiHashShort, txTime);
    }

    @Override
    public Transaction createNewCorrespondingDeXTTTransaction() {
        return new FinalizeTransaction(this);
    }

    @Override
    public byte[] convertToDeXTTPayload() {
        return super.convertToDeXTTPayload(FINALIZE_TRANSACTION_LENGTH, FINALIZE_TRANSACTION_TYPE);
    }

}
