package DeXTT.DataStructure;

import org.web3j.crypto.Sign;

public class ProofOfIntentFull {

    private ProofOfIntentData poiData;

    private Sign.SignatureData sigA;

    private Sign.SignatureData sigB;

    public ProofOfIntentFull(ProofOfIntentData poiData, Sign.SignatureData sigA, Sign.SignatureData sigB) {
        this.poiData = poiData;
        this.sigA = sigA;
        this.sigB = sigB;
    }

    public ProofOfIntentData getPoiData() {
        return poiData;
    }

    public Sign.SignatureData getSigA() {
        return sigA;
    }

    public Sign.SignatureData getSigB() {
        return sigB;
    }
}
