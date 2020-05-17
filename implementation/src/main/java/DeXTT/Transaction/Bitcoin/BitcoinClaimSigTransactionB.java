package DeXTT.Transaction.Bitcoin;

import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.Exception.AlreadyAddedTransactionException;
import DeXTT.Transaction.ClaimTransaction;
import DeXTT.Transaction.Transaction;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import static Configuration.Constants.CLAIM_SIG_TRANSACTION_B_LENGTH;
import static Configuration.Constants.CLAIM_SIG_TRANSACTION_B_TYPE;

public class BitcoinClaimSigTransactionB extends BitcoinClaimTransaction {

    private Sign.SignatureData signatureData;

    public BitcoinClaimSigTransactionB(Date txTime, int confirmations, Sign.SignatureData signatureData, BigInteger poiHashShort, DeXTTAddress bitcoinTransactionSender) {
        super(txTime, confirmations, poiHashShort, bitcoinTransactionSender);
        this.signatureData = signatureData;
    }

    public BitcoinClaimSigTransactionB(Sign.SignatureData signatureData, BigInteger poiHashShort) {
        super(null, -1, poiHashShort, null);
        this.signatureData = signatureData;
    }

    @Override
    public Transaction createNewCorrespondingDeXTTTransaction() {
        return new ClaimTransaction(this);
    }

    @Override
    public List<Transaction> tryToAddDeXTTBitcoinTransaction(Transaction transaction) throws AlreadyAddedTransactionException {
        return transaction.tryToAddDeXTTBitcoinTransaction(this);
    }

    @Override
    public byte[] convertToDeXTTPayload() {
        return super.convertToDeXTTPayload(CLAIM_SIG_TRANSACTION_B_LENGTH, CLAIM_SIG_TRANSACTION_B_TYPE, this.signatureData);
    }

    public Sign.SignatureData getSignatureData() {
        return signatureData;
    }
}
