package Events;

import DeXTT.DataStructure.ProofOfIntentFull;

import java.math.BigInteger;
import java.util.Date;

public class VetoContestStartedEvent extends WalletEvent {

    private BigInteger originalAlphaData;

    private ProofOfIntentFull originalPoi;

    private Date vetoEndTime;

    public VetoContestStartedEvent(BigInteger originalAlphaData, ProofOfIntentFull originalPoi, BigInteger alphaData, ProofOfIntentFull poi, Date vetoEndTime) {
        super(alphaData, poi);
        this.originalAlphaData = originalAlphaData;
        this.originalPoi = originalPoi;
        this.vetoEndTime = vetoEndTime;
    }

    public BigInteger getOriginalAlphaData() {
        return originalAlphaData;
    }

    public ProofOfIntentFull getOriginalPoi() {
        return originalPoi;
    }

    public Date getVetoEndTime() {
        return vetoEndTime;
    }
}
