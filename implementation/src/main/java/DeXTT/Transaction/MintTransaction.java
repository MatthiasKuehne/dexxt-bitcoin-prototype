package DeXTT.Transaction;

import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.Wallet;
import DeXTT.Transaction.Bitcoin.BitcoinMintTransaction;
import DeXTT.Transaction.Bitcoin.BitcoinTransaction;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class MintTransaction implements Transaction {

    private DeXTTAddress receiver;

    private BigInteger amount;

    private DeXTTAddress bitcoinTransactionSender;

    public MintTransaction(DeXTTAddress receiver, BigInteger amount) {
        this.receiver = receiver;
        this.amount = amount;
    }

    public MintTransaction(DeXTTAddress receiver, BigInteger amount, DeXTTAddress bitcoinTransactionSender) {
        this(receiver, amount);
        this.bitcoinTransactionSender = bitcoinTransactionSender;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public List<BitcoinTransaction> convertToDeXTTBitcoinTransactions() {
        ArrayList<BitcoinTransaction> transactions = new ArrayList<>();
        transactions.add(new BitcoinMintTransaction(this.receiver, this.amount));

        return transactions;
    }

    @Override
    public void tryToExecute(Wallet wallet) {
        wallet.executeMintTransaction(this);
    }

    public DeXTTAddress getReceiver() {
        return this.receiver;
    }

    public BigInteger getAmount() {
        return this.amount;
    }

    public DeXTTAddress getBitcoinTransactionSender() {
        return bitcoinTransactionSender;
    }

    @Override
    public boolean canBeExecutedUnconfirmed() {
        return true;
    }
}
