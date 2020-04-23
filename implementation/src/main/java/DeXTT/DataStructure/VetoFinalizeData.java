package DeXTT.DataStructure;

import java.util.Date;

public class VetoFinalizeData implements Comparable<VetoFinalizeData> {

    private Date endTime;

    private DeXTTAddress conflictingPoiSender;

    public VetoFinalizeData(Date endTime, DeXTTAddress conflictingPoiSender) {
        this.endTime = endTime;
        this.conflictingPoiSender = conflictingPoiSender;
    }

    public Date getEndTime() {
        return endTime;
    }

    public DeXTTAddress getConflictingPoiSender() {
        return conflictingPoiSender;
    }

    @Override
    public int compareTo(VetoFinalizeData that) {
        if (that == null) {
            throw new NullPointerException();
        }

        long thisTime = this.endTime.getTime();
        long thatTime = that.endTime.getTime();

        if (thisTime < thatTime) {
            return -1;
        } else if (thisTime > thatTime) {
            return 1;
        } else {
            // same time.. use address to get ordering
            return this.conflictingPoiSender.getAddressWithoutPrefix().compareTo(that.conflictingPoiSender.getAddressWithoutPrefix());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }

        VetoFinalizeData that = (VetoFinalizeData) obj;

        return (this.endTime.equals(that.endTime) && this.conflictingPoiSender.equals(that.conflictingPoiSender));
    }
}
