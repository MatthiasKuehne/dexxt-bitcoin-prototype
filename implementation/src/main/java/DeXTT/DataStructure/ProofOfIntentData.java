package DeXTT.DataStructure;

import Communication.RMI.ProofOfIntentRMI;

import java.math.BigInteger;
import java.util.Date;

public class ProofOfIntentData {

    private DeXTTAddress sender;

    private DeXTTAddress receiver;

    private BigInteger amount;

    private Date startTime;

    private Date endTime;

    public ProofOfIntentData(DeXTTAddress sender, DeXTTAddress receiver, BigInteger amount, Date startTime, Date endTime) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public ProofOfIntentData(ProofOfIntentRMI poiRMI) {
        this.sender = poiRMI.getSender();
        this.receiver = poiRMI.getReceiver();
        this.amount = poiRMI.getAmount();
        this.startTime = poiRMI.getStartTime();
        this.endTime = poiRMI.getEndTime();
    }

    public DeXTTAddress getSender() {
        return sender;
    }

    public void setSender(DeXTTAddress sender) {
        this.sender = sender;
    }

    public DeXTTAddress getReceiver() {
        return receiver;
    }

    public void setReceiver(DeXTTAddress receiver) {
        this.receiver = receiver;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public void setAmount(BigInteger amount) {
        this.amount = amount;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }

        ProofOfIntentData that = (ProofOfIntentData) obj;

        return (this.sender.equals(that.sender)
                && this.receiver.equals(that.receiver)
                && this.amount.equals(that.amount)
                && this.startTime.equals(that.startTime)
                && this.endTime.equals(that.endTime));
    }
}
