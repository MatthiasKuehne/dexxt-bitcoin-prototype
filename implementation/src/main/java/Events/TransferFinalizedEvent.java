package Events;

import DeXTT.DataStructure.ProofOfIntentFull;

import java.math.BigInteger;

public class TransferFinalizedEvent extends WalletEvent {

    public TransferFinalizedEvent(BigInteger alphaData, ProofOfIntentFull poi) {
        super(alphaData, poi);
    }
}
