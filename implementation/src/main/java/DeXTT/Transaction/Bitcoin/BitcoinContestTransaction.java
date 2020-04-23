package DeXTT.Transaction.Bitcoin;

import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.Transaction.ContestTransaction;
import DeXTT.Transaction.Transaction;

import java.math.BigInteger;
import java.util.Date;

import static Configuration.Constants.*;

public class BitcoinContestTransaction extends BitcoinHashReferenceTransaction {

    private DeXTTAddress bitcoinTransactionSender;

//    private Date txTime;

    private int confirmations;

    public BitcoinContestTransaction(BigInteger poiHashFull, BigInteger poiHashShort, DeXTTAddress bitcoinTransactionSender, Date txTime, int confirmations) {
        super(poiHashFull, poiHashShort, txTime);
        this.bitcoinTransactionSender = bitcoinTransactionSender;
        this.confirmations = confirmations;
    }

    public BitcoinContestTransaction(BigInteger poiHashFull, BigInteger poiHashShort) {
        super(poiHashFull, poiHashShort, null);
        this.bitcoinTransactionSender = null;
        this.confirmations = -1;
    }

    @Override
    public Transaction createNewCorrespondingDeXTTTransaction() {
        return new ContestTransaction(this, this.bitcoinTransactionSender, this.getTxTime(), this.confirmations);
    }

    @Override
    public byte[] convertToDeXTTPayload() {
        return super.convertToDeXTTPayload(CONTEST_PARTICIPATION_TRANSACTION_LENGTH, CONTEST_PARTICIPATION_TRANSACTION_TYPE);
    }

    public DeXTTAddress getBitcoinTransactionSender() {
        return bitcoinTransactionSender;
    }
}
