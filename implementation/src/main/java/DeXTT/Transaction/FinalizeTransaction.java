package DeXTT.Transaction;

import DeXTT.Cryptography;
import DeXTT.DataStructure.ProofOfIntentData;
import DeXTT.Transaction.Bitcoin.BitcoinFinalizeTransaction;
import DeXTT.Transaction.Bitcoin.BitcoinHashReferenceTransaction;
import DeXTT.Wallet;

import java.math.BigInteger;

public class FinalizeTransaction extends HashReferenceTransaction {

    public FinalizeTransaction(BitcoinHashReferenceTransaction bitcoinTransaction) {
        super(bitcoinTransaction);
    }

    public FinalizeTransaction(ProofOfIntentData poi) {
        super(new BitcoinFinalizeTransaction(Cryptography.calculateFullPoiHash(poi), Cryptography.calculateShortPoiHash(poi), null));
    }

    public FinalizeTransaction(BigInteger poiHashFull) {
        super(new BitcoinFinalizeTransaction(poiHashFull, null, null));
    }

    @Override
    public void tryToExecute(Wallet wallet) {
        wallet.executeFinalizeTransaction(this);
    }
}
