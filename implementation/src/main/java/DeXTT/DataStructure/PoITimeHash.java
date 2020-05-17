package DeXTT.DataStructure;

import java.math.BigInteger;
import java.util.Date;

public class PoITimeHash implements Comparable<PoITimeHash> {

    private Date endTime;

    private BigInteger poiHash;

    public PoITimeHash(Date endTime, BigInteger poiHash) {
        this.endTime = endTime;
        this.poiHash = poiHash;
    }

    public Date getEndTime() {
        return endTime;
    }

    public BigInteger getPoiHash() {
        return poiHash;
    }

    @Override
    public int compareTo(PoITimeHash that) {
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
            // same time.. use hash to get ordering
            return this.poiHash.compareTo(that.poiHash);
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

        PoITimeHash that = (PoITimeHash) obj;

        return (this.endTime.equals(that.endTime) && this.poiHash.equals(that.poiHash));
    }
}
