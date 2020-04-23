package DeXTT;

import DeXTT.Transaction.Transaction;

import java.math.BigInteger;
import java.util.*;

public class TransactionManager {

    // only save/store TX from Blockchain, not the ones that get out

    private Map<BigInteger, List<Transaction>> incompleteTransactions; // maps 8 byte PoI Hash

    private Set<String> processedUnconfirmedTxIds;

    private Set<String> readUnconfirmedTxIds;

    public TransactionManager() {
        this.incompleteTransactions = new HashMap<>();
        this.processedUnconfirmedTxIds = new LinkedHashSet<>();
        this.readUnconfirmedTxIds = new LinkedHashSet<>();
    }

    /**
     *
     * @param poiHash
     * @return Empty list if nothing found.
     */
    public List<Transaction> getMatchingTransactions(BigInteger poiHash) {
        return this.incompleteTransactions.getOrDefault(poiHash, new ArrayList<>());
    }

    public void addIncompleteTransaction(BigInteger poiHash, Transaction transaction) {
        List<Transaction> transactions = this.incompleteTransactions.computeIfAbsent(poiHash, k -> new ArrayList<>()); // init if not existing poi

        boolean containsTransactionAlready = false;
        for (Transaction tx: transactions) { // check if already in list
            if (tx == transaction) { // == comparision wanted -> check if same object reference
                containsTransactionAlready = true;
                break;
            }
        }

        if (!containsTransactionAlready) {
            transactions.add(transaction);
        }
    }

    public void markAsCompleted(BigInteger poiHash, Transaction transaction) {
        List<Transaction> transactions = this.incompleteTransactions.get(poiHash);
        if (transactions != null) {
            int indexToRemove = -1;
            for (int index = 0; index < transactions.size() && indexToRemove < 0; index++) {
                if (transactions.get(index) == transaction) { // == comparision wanted -> check if same object reference
                    // found transaction
                    indexToRemove = index;
                }
            }
            if (indexToRemove >= 0) {
                transactions.remove(indexToRemove);
                if (transactions.size() == 0) {
                    // remove entry
                    this.incompleteTransactions.remove(poiHash);
                }
            }
        }
    }

    public void addReadUnconfirmedTxIds(Set<String> txIds) {
        this.readUnconfirmedTxIds.addAll(txIds);
    }

    public Set<String> getReadUnconfirmedTxIds() {
        return this.readUnconfirmedTxIds;
    }

    public void clearReadUnconfirmedTxIds() {
        this.readUnconfirmedTxIds.clear();
    }

    public void addProcessedUnconfirmedTxId(String txId) {
        this.processedUnconfirmedTxIds.add(txId);
    }

    public void removeProcessedUnconfirmedTxId(String txId) {
        this.processedUnconfirmedTxIds.remove(txId);
    }

    public boolean containsProcessedUnconfirmedTxId(String txId) {
        return this.processedUnconfirmedTxIds.contains(txId);
    }
}
