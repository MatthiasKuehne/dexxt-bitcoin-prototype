package DeXTT.Transaction;

import DeXTT.Transaction.Bitcoin.BitcoinHashReferenceTransaction;
import DeXTT.Transaction.Bitcoin.BitcoinTransaction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class HashReferenceTransaction implements Transaction {

    private BitcoinHashReferenceTransaction bitcoinTransaction;

    public HashReferenceTransaction(BitcoinHashReferenceTransaction bitcoinTransaction) {
        this.bitcoinTransaction = bitcoinTransaction;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public List<BitcoinTransaction> convertToDeXTTBitcoinTransactions() {
        ArrayList<BitcoinTransaction> transactions = new ArrayList<>();
        transactions.add(this.bitcoinTransaction);

        return transactions;
    }

    public BitcoinHashReferenceTransaction getBitcoinTransaction() {
        return bitcoinTransaction;
    }

    public BigInteger getPoiHashFull() {
        return bitcoinTransaction.getPoiHashFull();
    }

    public Date getTxTime() {
        return this.bitcoinTransaction.getTxTime();
    }
}
