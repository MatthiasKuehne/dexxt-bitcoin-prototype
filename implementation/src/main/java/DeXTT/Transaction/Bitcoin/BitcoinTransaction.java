package DeXTT.Transaction.Bitcoin;

import DeXTT.Exception.AlreadyAddedTransactionException;
import DeXTT.Transaction.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public interface BitcoinTransaction {

    static final Logger logger = LogManager.getLogger();

    /**
     * 8 bytes hash of PoI of corresponding DeXTT Transaction
     * @return
     */
    default BigInteger getPoiHashShort() {
        return null;
    }

    /**
     * @param transactions      already existing DeXTT transactions (PoI Hash matching)
     *                          might also be null/empty, if nothing exists yet
     * @return                  DeXTT TX containing the data from the DeXTTBitcoin Transaction
     */
    default List<Transaction> putIntoDeXTTTransaction(List<Transaction> transactions) {
        List<Transaction> matchedTransactionsAll = new ArrayList<>();
        try {
            for (Transaction transaction : transactions) {
                List<Transaction> matchedTransactions = null;
                matchedTransactions = tryToAddDeXTTBitcoinTransaction(transaction);
                if (matchedTransactions != null) {
                    matchedTransactionsAll.addAll(matchedTransactions);
                }
            }

            logger.debug("Matched Transaction: " + matchedTransactionsAll.size());

            if (matchedTransactionsAll.size() == 0) {
                // create new DeXTTTransaction
                logger.debug("No matched Transactions, creating new one.");
                Transaction newlyCreatedTransaction = createNewCorrespondingDeXTTTransaction();
                matchedTransactionsAll.add(newlyCreatedTransaction);
            }
        } catch (AlreadyAddedTransactionException e) {
            // "this" Transaction was already added to a DeXTTTransaction, no need to match or create new Transactions: ignore it
            return new ArrayList<>(); // an already added transaction should return no match -> discard transaction
        }

        return matchedTransactionsAll;
    }

    Transaction createNewCorrespondingDeXTTTransaction();

    /**
     * Binds object dynamically to call correct method in DeXTTTransaction
     * @param transaction
     * @return  null if nothing can be added
     */
    default List<Transaction> tryToAddDeXTTBitcoinTransaction(Transaction transaction) throws AlreadyAddedTransactionException {
        return null;
    }

    /**
     * inclusive "DeXTT" TX Prefix, ready to send
     * @return
     */
    byte[] convertToDeXTTPayload();
}
