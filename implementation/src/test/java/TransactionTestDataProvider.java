import Configuration.Chain;
import Configuration.ConfigCommand;
import Configuration.Configuration;
import DeXTT.Cryptography;
import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.DataStructure.ProofOfIntentData;
import DeXTT.Exception.AlreadyAddedTransactionException;
import DeXTT.Transaction.Bitcoin.BitcoinClaimDataTransaction;
import DeXTT.Transaction.Bitcoin.BitcoinClaimSigTransactionA;
import DeXTT.Transaction.Bitcoin.BitcoinClaimSigTransactionB;
import DeXTT.Transaction.ClaimTransaction;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;

public abstract class TransactionTestDataProvider {

    protected BitcoinClaimDataTransaction claimDataTx;
    protected BitcoinClaimSigTransactionA sigATx;
    protected BitcoinClaimSigTransactionB sigBTx;
    protected ClaimTransaction claimTransaction;

    public TransactionTestDataProvider() {
        ConfigCommand configCommand = new ConfigCommand();
        configCommand.chain = Chain.REGTEST; // should be same as MINTING_ADDRESS -> otherwise minting does not work...?
        Configuration.init(configCommand);
    }

    protected void createAndSaveClaimTransactionParts() {
        // BTC sender: "bcrt1qejv7wf8ax4lkag9w3hclyzu47zgj7q6eqv6s60"
        String privateKeyWifSender = "cRCFqu3zcDHZoSqWdwhwgEj3gCLSbEtLMp2TzJudebnSKvP7Shvn";
        DeXTTAddress sender = Cryptography.createAddressFromWIFPrivateKey(privateKeyWifSender);
        ECKeyPair keySender = Cryptography.createSigningKeyFromWIFPrivateKey(privateKeyWifSender);

        // BTC receiver: "bcrt1q30t4dep9laf3k8ys84xrx752awwuzt97gsvc3d"
        String privateKeyWifReceiver = "cMbQWoFkDbhWZ2H2SCAYLrY59Uvh3NnJUndoCXa79ak5qEfTfKsb";
        DeXTTAddress receiver = Cryptography.createAddressFromWIFPrivateKey(privateKeyWifReceiver);
        ECKeyPair keyReceiver = Cryptography.createSigningKeyFromWIFPrivateKey(privateKeyWifReceiver);

        BigInteger amount = BigInteger.valueOf(10);
        Date t0 = Date.from(Instant.now());
        long intervalMillis = 10000; // 10s
        Date t1 = new Date(t0.getTime() + intervalMillis);

        Date txTime = new Date(t0.getTime() + (intervalMillis / 2));

        ProofOfIntentData poiData = new ProofOfIntentData(sender, receiver, amount, t0, t1);
        BigInteger poiHashShort = Cryptography.calculateShortPoiHash(poiData);
        Sign.SignatureData sigA = Cryptography.signPoi(poiData, keySender);
        Sign.SignatureData sigB = Cryptography.signAlphaData(sigA, keyReceiver);
        this.claimDataTx = new BitcoinClaimDataTransaction(txTime, 1, sender, receiver, amount, t0, t1, poiHashShort, receiver);
        this.sigATx = new BitcoinClaimSigTransactionA(txTime, 1, sigA, poiHashShort, receiver);
        this.sigBTx = new BitcoinClaimSigTransactionB(txTime, 1, sigB, poiHashShort, receiver);
    }

    protected void createAndSaveClaimTransaction() throws AlreadyAddedTransactionException {
        this.createAndSaveClaimTransactionParts();

        ClaimTransaction claimTransaction = new ClaimTransaction(this.claimDataTx);
        claimTransaction.tryToAddDeXTTBitcoinTransaction(this.sigATx);
        claimTransaction.tryToAddDeXTTBitcoinTransaction(this.sigBTx);
        this.claimTransaction = claimTransaction;
    }

}
