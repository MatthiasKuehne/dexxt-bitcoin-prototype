package DeXTT.Transaction;

import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.DataStructure.ProofOfIntentFull;
import DeXTT.Wallet;
import DeXTT.Exception.AlreadyAddedTransactionException;
import DeXTT.Exception.UnconfirmedTransactionExecutionException;
import DeXTT.DataStructure.ProofOfIntentData;
import DeXTT.Transaction.Bitcoin.BitcoinClaimDataTransaction;
import DeXTT.Transaction.Bitcoin.BitcoinClaimSigTransactionA;
import DeXTT.Transaction.Bitcoin.BitcoinClaimSigTransactionB;
import DeXTT.Transaction.Bitcoin.BitcoinTransaction;
import DeXTT.Cryptography;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ClaimTransaction implements Transaction {

    // include blocktime of latest/last part of claim TX
    private Date txTime;

    // smallest confirmations of all part TXs
    private int confirmations;

    private DeXTTAddress BitcoinTransactionSender;

    private BitcoinClaimDataTransaction poiDataTx;

    private BitcoinClaimSigTransactionA signatureATx;

    private BitcoinClaimSigTransactionB signatureBTx;

    public ClaimTransaction(BitcoinClaimDataTransaction transaction) {
        this.txTime = transaction.getTxTime();
        this.confirmations = transaction.getConfirmations();
        this.poiDataTx = transaction;
        this.BitcoinTransactionSender = transaction.getBitcoinTransactionSender();

        this.signatureATx = null;
        this.signatureBTx = null;
    }

    public ClaimTransaction(BitcoinClaimSigTransactionA transaction) {
        this.txTime = transaction.getTxTime();
        this.confirmations = transaction.getConfirmations();
        this.signatureATx = transaction;
        this.BitcoinTransactionSender = transaction.getBitcoinTransactionSender();

        this.poiDataTx = null;
        this.signatureBTx = null;
    }

    public ClaimTransaction(BitcoinClaimSigTransactionB transaction) {
        this.txTime = transaction.getTxTime();
        this.confirmations = transaction.getConfirmations();
        this.signatureBTx = transaction;
        this.BitcoinTransactionSender = transaction.getBitcoinTransactionSender();

        this.poiDataTx = null;
        this.signatureATx = null;
    }

    /**
     * constructor for creation after RMI receive thing
     * @param poiData
     * @param sigA
     * @param sigB
     * @param poiHashShort
     */
    public ClaimTransaction(ProofOfIntentData poiData, Sign.SignatureData sigA, Sign.SignatureData sigB, BigInteger poiHashShort) {
        this.poiDataTx = new BitcoinClaimDataTransaction(poiData, poiHashShort);
        this.signatureATx = new BitcoinClaimSigTransactionA(sigA, poiHashShort);
        this.signatureBTx = new BitcoinClaimSigTransactionB(sigB, poiHashShort);
        this.confirmations = -1;
        this.txTime = null;
        this.BitcoinTransactionSender = null;
    }

    @Override
    public boolean isComplete() {
        return (this.poiDataTx != null && this.signatureATx != null && this.signatureBTx != null);
    }

    @Override
    public List<Transaction> tryToAddDeXTTBitcoinTransaction(BitcoinClaimDataTransaction claimDataTransaction) throws AlreadyAddedTransactionException {
        List<Transaction> matches = new ArrayList<>();

        if (this.BitcoinTransactionSender.equals(claimDataTransaction.getBitcoinTransactionSender())) { // can only match if same BTC TX sender
            if (this.poiDataTx != null) { // already added a poi
                if (this.poiDataTx.getPoi().equals(claimDataTransaction.getPoi())) { // same poi data
                    if (this.poiDataTx.getConfirmations() <= 0 && claimDataTransaction.getConfirmations() > 0) { // check if it is now confirmed
                        // match: same poi -> no checks needed
                        this.addClaimData(matches, claimDataTransaction);
                    } else {
                        // same poi, no "better" confirmations -> throw exception
                        throw new AlreadyAddedTransactionException("Transaction already contains same DeXTTBitcoinClaimDataTransaction");
                    }
                }
            } else {
                // try to match and set ClaimData
                if (this.signatureATx == null || Cryptography.verifySigA(claimDataTransaction.getPoi(), this.signatureATx.getSignatureData())) {
                    // no signature available OR signature match => add transaction
                    // possible that claimData is added to NON-MATCHING signatureB!!
                    this.addClaimData(matches, claimDataTransaction);
                }
            }
        }

        return matches;
    }

    private void addClaimData(List<Transaction> matches, BitcoinClaimDataTransaction claimDataTransaction) {
        this.poiDataTx = claimDataTransaction;
        this.setConfirmationsAndTime(claimDataTransaction.getConfirmations(), claimDataTransaction.getTxTime());
        matches.add(this);
    }

    @Override
    public List<Transaction> tryToAddDeXTTBitcoinTransaction(BitcoinClaimSigTransactionA claimSigTransactionA) throws AlreadyAddedTransactionException {
        List<Transaction> matches = new ArrayList<>();

        if (this.BitcoinTransactionSender.equals(claimSigTransactionA.getBitcoinTransactionSender())) { // can only match if same BTC TX sender
            if (this.signatureATx != null) { // already added signature
                if (this.signatureATx.getSignatureData().equals(claimSigTransactionA.getSignatureData())) { // same signature data
                    if (this.signatureATx.getConfirmations() <= 0 && claimSigTransactionA.getConfirmations() > 0) { // was unconfirmed, now confirmed
                        // match: same signature -> no checks needed
                        this.addClaimSigA(matches, claimSigTransactionA);
                    } else {
                        throw new AlreadyAddedTransactionException("Transaction already contains same DeXTTBitcoinClaimSigTransactionA");
                    }
                }
            } else {
                // try to match and set signature
                if (this.poiDataTx == null) {
                    // can't check any signatures -> add
                    this.addClaimSigA(matches, claimSigTransactionA);
                } else {
                    boolean matchSigA = Cryptography.verifySigA(this.poiDataTx.getPoi(), claimSigTransactionA.getSignatureData());
                    if (this.signatureBTx == null) {
                        if (matchSigA) {
                            this.addClaimSigA(matches, claimSigTransactionA);
                        }
                    } else {
                        boolean matchSigB = Cryptography.verifySigB(claimSigTransactionA.getSignatureData(), this.poiDataTx.getReceiver(), this.signatureBTx.getSignatureData());
                        if (matchSigA && matchSigB) {
                            this.addClaimSigA(matches, claimSigTransactionA);
                        } else if (matchSigA) { // sigA && !sigB
                            // add sigA, remove sigB into new TX
                            this.addClaimSigA(matches, claimSigTransactionA);
                            BitcoinClaimSigTransactionB sigBTx = this.signatureBTx;
                            this.signatureBTx = null;

                            matches.add(new ClaimTransaction(sigBTx));
                        } else if (matchSigB) {  // !sigA && sigB
                            // add sigA, remove poiData into new TX
                            this.addClaimSigA(matches, claimSigTransactionA);
                            BitcoinClaimDataTransaction poiTx = this.poiDataTx;
                            this.poiDataTx = null;

                            matches.add(new ClaimTransaction(poiTx));
                        } else { // !sigA && !sigB
                            // keep current TX as is (no match), create new TX with sigA + this.SigB
                            Transaction newTx = new ClaimTransaction(claimSigTransactionA);
                            newTx.tryToAddDeXTTBitcoinTransaction(this.signatureBTx);
                            matches.add(newTx);
                        }
                    }
                }
            }
        }

        return matches;
    }

    private void addClaimSigA(List<Transaction> matches, BitcoinClaimSigTransactionA claimSigTransactionA) {
        this.signatureATx = claimSigTransactionA;
        this.setConfirmationsAndTime(claimSigTransactionA.getConfirmations(), claimSigTransactionA.getTxTime());
        matches.add(this);
    }

    @Override
    public List<Transaction> tryToAddDeXTTBitcoinTransaction(BitcoinClaimSigTransactionB claimSigTransactionB) throws AlreadyAddedTransactionException {
        List<Transaction> matches = new ArrayList<>();

        if (this.BitcoinTransactionSender.equals(claimSigTransactionB.getBitcoinTransactionSender())) { // can only match if same BTC TX sender
            if (this.signatureBTx != null) { // already added signature
                if (this.signatureBTx.getSignatureData().equals(claimSigTransactionB.getSignatureData())) { // same signature data
                    if (this.signatureBTx.getConfirmations() <= 0 && claimSigTransactionB.getConfirmations() > 0) { // was unconfirmed, now confirmed
                        // match: same signature -> no checks needed
                        this.addClaimSigB(matches, claimSigTransactionB);
                    } else {
                        throw new AlreadyAddedTransactionException("Transaction already contains same DeXTTBitcoinClaimSigTransactionB");
                    }
                }
            } else {
                // try to match and set signature
                if (this.poiDataTx == null || this.signatureATx == null || Cryptography.verifySigB(this.signatureATx.getSignatureData(), this.poiDataTx.getReceiver(), claimSigTransactionB.getSignatureData())) {
                    // can only check if both poiData and sigA is present!!!! -> possible that sigB is added to TX with wrong poiData or sigA
                    this.addClaimSigB(matches, claimSigTransactionB);
                }
            }
        }

        return matches;
    }

    private void addClaimSigB(List<Transaction> matches, BitcoinClaimSigTransactionB claimSigTransactionB) {
        this.signatureBTx = claimSigTransactionB;
        this.setConfirmationsAndTime(claimSigTransactionB.getConfirmations(), claimSigTransactionB.getTxTime());
        matches.add(this);
    }

    @Override
    public List<BitcoinTransaction> convertToDeXTTBitcoinTransactions() {
        List<BitcoinTransaction> transactions = new ArrayList<>();
        transactions.add(this.poiDataTx);
        transactions.add(this.signatureATx);
        transactions.add(this.signatureBTx);
        return transactions;
    }

    @Override
    public void tryToExecute(Wallet wallet) throws UnconfirmedTransactionExecutionException {
        if (!this.isComplete()) {
            return; // should not happen
        }

        wallet.executeClaimContestTransaction(this);
    }

    private void setConfirmationsAndTime(int newConfirmations, Date newTime) {
        if (this.poiDataTx != null && this.signatureATx != null && this.signatureBTx != null) {
            // all TX set, just take minimum of confirmations
            this.confirmations = Math.min(Math.min(poiDataTx.getConfirmations(), signatureATx.getConfirmations()), signatureBTx.getConfirmations());
        } else {
            if (newConfirmations < this.confirmations) {
                this.confirmations = newConfirmations;
            }
        }

        if (this.txTime.before(newTime)) {
            this.txTime = newTime;
        }
    }

    public ProofOfIntentFull getPoiFull() {
        return new ProofOfIntentFull(this.poiDataTx.getPoi(), this.signatureATx.getSignatureData(), this.signatureBTx.getSignatureData());
    }

    public DeXTTAddress getBitcoinTransactionSender() {
        return BitcoinTransactionSender;
    }

    public Date getTxTime() {
        return txTime;
    }

    public int getConfirmations() {
        return confirmations;
    }

    private int getMaximalConfirmations() {
        return Math.max(Math.max(poiDataTx.getConfirmations(), signatureATx.getConfirmations()), signatureBTx.getConfirmations());
    }

    @Override
    public boolean resetToConfirmationsOnly() {
        if (this.poiDataTx != null && this.poiDataTx.getConfirmations() <= 0) {
            this.poiDataTx = null;
        }
        if (this.signatureATx != null && this.signatureATx.getConfirmations() <= 0) {
            this.signatureATx = null;
        }
        if (this.signatureBTx != null && this.signatureBTx.getConfirmations() <= 0) {
            this.signatureBTx = null;
        }

        List<Integer> confirmations = new ArrayList<>();
        List<Date> times = new ArrayList<>();
        if (this.poiDataTx != null) {
            confirmations.add(this.poiDataTx.getConfirmations());
            times.add(this.poiDataTx.getTxTime());
        }
        if (this.signatureATx != null) {
            confirmations.add(this.signatureATx.getConfirmations());
            times.add(this.signatureATx.getTxTime());
        }
        if (this.signatureBTx != null) {
            confirmations.add(this.signatureBTx.getConfirmations());
            times.add(this.signatureBTx.getTxTime());
        }

        if (confirmations.size() == 0 || times.size() == 0) {
            // nothing left
            this.confirmations = 0;
            return false;
        } else {
            int minConfirmations = confirmations.stream().mapToInt(a -> a).min().orElse(0); // default to unconfirmed...
            Date maxTime = Collections.max(times);
            if (minConfirmations == 0) {
                return false;
            } else {
                this.confirmations = minConfirmations;
                this.txTime = maxTime;
                return true;
            }
        }
    }
}