package DeXTT.Transaction;

import DeXTT.Cryptography;
import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.DataStructure.ProofOfIntentData;
import DeXTT.Exception.FullClaimMissingException;
import DeXTT.Exception.UnconfirmedTransactionExecutionException;
import DeXTT.Transaction.Bitcoin.BitcoinContestTransaction;
import DeXTT.Transaction.Bitcoin.BitcoinHashReferenceTransaction;
import DeXTT.Wallet;

import java.util.Date;

// Also Veto, as in Ethereum
public class ContestTransaction extends HashReferenceTransaction {

    private DeXTTAddress bitcoinTransactionSender;

    private int confirmations;

    public ContestTransaction(BitcoinHashReferenceTransaction bitcoinTransaction, DeXTTAddress bitcoinTransactionSender, Date txTime, int confirmations) {
        super(bitcoinTransaction);
        this.bitcoinTransactionSender = bitcoinTransactionSender;
        this.confirmations = confirmations;
    }

    public ContestTransaction(ProofOfIntentData poi) {
        super(new BitcoinContestTransaction(Cryptography.calculateFullPoiHash(poi), Cryptography.calculateShortPoiHash(poi)));
        this.bitcoinTransactionSender = null;
        this.confirmations = -1;
    }

    @Override
    public void tryToExecute(Wallet wallet) throws UnconfirmedTransactionExecutionException, FullClaimMissingException {
        wallet.executeContestHashReferenceTransaction(this);
    }

    public DeXTTAddress getBitcoinTransactionSender() {
        return bitcoinTransactionSender;
    }

    public int getConfirmations() {
        return confirmations;
    }
}
