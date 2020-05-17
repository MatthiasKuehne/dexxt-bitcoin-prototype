package Events;

import DeXTT.DataStructure.DeXTTAddress;

import java.util.Date;

public class VetoFinalizedEvent {

    private DeXTTAddress conflictingPoiSender;

    private Date vetoEndTime;

    public VetoFinalizedEvent(DeXTTAddress conflictingPoiSender, Date vetoEndTime) {
        this.conflictingPoiSender = conflictingPoiSender;
        this.vetoEndTime = vetoEndTime;
    }

    public DeXTTAddress getConflictingPoiSender() {
        return conflictingPoiSender;
    }

    public Date getVetoEndTime() {
        return vetoEndTime;
    }
}
