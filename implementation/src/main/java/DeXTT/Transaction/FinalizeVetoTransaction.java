package DeXTT.Transaction;

import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.Transaction.Bitcoin.BitcoinFinalizeVetoTransaction;
import DeXTT.Transaction.Bitcoin.BitcoinTransaction;
import DeXTT.Wallet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FinalizeVetoTransaction implements Transaction {

    private Date txTime;

    private DeXTTAddress conflictingPoiSender;

    public FinalizeVetoTransaction(DeXTTAddress conflictingPoiSender, Date txTime) {
        this.txTime = txTime;
        this.conflictingPoiSender = conflictingPoiSender;
    }

    public FinalizeVetoTransaction(DeXTTAddress conflictingPoiSender) {
        this.conflictingPoiSender = conflictingPoiSender;
        this.txTime = null;
    }

    @Override
    public void tryToExecute(Wallet wallet) {
        wallet.executeFinalizeVetoTransaction(this);
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public List<BitcoinTransaction> convertToDeXTTBitcoinTransactions() {
        ArrayList<BitcoinTransaction> transactions = new ArrayList<>();
        transactions.add(new BitcoinFinalizeVetoTransaction(this.conflictingPoiSender, this.txTime));
        return transactions;
    }

    public Date getTxTime() {
        return txTime;
    }

    public DeXTTAddress getConflictingPoiSender() {
        return conflictingPoiSender;
    }
}
