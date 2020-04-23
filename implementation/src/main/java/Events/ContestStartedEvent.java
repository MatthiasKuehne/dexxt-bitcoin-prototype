package Events;

import DeXTT.DataStructure.ProofOfIntentFull;

import java.math.BigInteger;

public class ContestStartedEvent extends WalletEvent{

    public ContestStartedEvent(BigInteger alphaData, ProofOfIntentFull poi) {
        super(alphaData, poi);
    }
}
