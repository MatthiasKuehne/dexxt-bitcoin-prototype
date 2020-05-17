package DeXTT.Transaction;

import DeXTT.Exception.AlreadyAddedTransactionException;
import DeXTT.Exception.FullClaimMissingException;
import DeXTT.Exception.UnconfirmedTransactionExecutionException;
import DeXTT.Transaction.Bitcoin.BitcoinClaimDataTransaction;
import DeXTT.Transaction.Bitcoin.BitcoinClaimSigTransactionA;
import DeXTT.Transaction.Bitcoin.BitcoinClaimSigTransactionB;
import DeXTT.Transaction.Bitcoin.BitcoinTransaction;
import DeXTT.Wallet;

import java.util.List;

public interface Transaction {

    boolean isComplete();

    /**
     *
     * @param claimDataTransaction
     * @return      Transaction with added data
     *              null if transaction does not match
     */
    default List<Transaction> tryToAddDeXTTBitcoinTransaction(BitcoinClaimDataTransaction claimDataTransaction) throws AlreadyAddedTransactionException  {
        return null;
    }

    default List<Transaction> tryToAddDeXTTBitcoinTransaction(BitcoinClaimSigTransactionA claimSigTransactionA) throws AlreadyAddedTransactionException {
        return null;
    }

    default List<Transaction> tryToAddDeXTTBitcoinTransaction(BitcoinClaimSigTransactionB claimSigTransactionB) throws AlreadyAddedTransactionException {
        return null;
    }

    // for other direction
    List<BitcoinTransaction> convertToDeXTTBitcoinTransactions();

    void tryToExecute(Wallet wallet) throws UnconfirmedTransactionExecutionException, FullClaimMissingException;

    default boolean canBeExecutedUnconfirmed() {
        return false;
    }

    /**
     *
     * @return true, if reset ended in parts being still there (confirmed parts)
     *          false, if transaction is now empty -> can be delete externally
     */
    default boolean resetToConfirmationsOnly() {
        return false;
    }
}
