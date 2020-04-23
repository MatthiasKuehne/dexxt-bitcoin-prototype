package Events;

import DeXTT.DataStructure.ProofOfIntentFull;

import java.math.BigInteger;

public abstract class WalletEvent {

    private BigInteger alphaData;

    private ProofOfIntentFull poi;

    public WalletEvent(BigInteger alphaData, ProofOfIntentFull poi) {
        this.alphaData = alphaData;
        this.poi = poi;
    }

    public BigInteger getAlphaData() {
        return alphaData;
    }

    public ProofOfIntentFull getPoi() {
        return poi;
    }

}
