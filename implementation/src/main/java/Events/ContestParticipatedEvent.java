package Events;

import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.DataStructure.ProofOfIntentFull;

import java.math.BigInteger;

public class ContestParticipatedEvent extends WalletEvent {

    DeXTTAddress participant;

    public ContestParticipatedEvent(BigInteger alphaData, ProofOfIntentFull poi, DeXTTAddress participant) {
        super(alphaData, poi);
        this.participant = participant;
    }

    public DeXTTAddress getParticipant() {
        return participant;
    }
}
