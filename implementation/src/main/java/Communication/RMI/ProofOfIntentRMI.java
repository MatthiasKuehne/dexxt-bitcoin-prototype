package Communication.RMI;

import DeXTT.DataStructure.DeXTTAddress;
import org.web3j.crypto.Sign;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

public class ProofOfIntentRMI implements Serializable {

    private int version;

    private String sender;

    private String receiver;

    private BigInteger amount;

    private Date startTime;

    private Date endTime;

    private byte[] sigA_v;

    private byte[] sigA_r;

    private byte[] sigA_s;

    private ProofOfIntentRMI(int version, String sender, String receiver, BigInteger amount, Date startTime, Date endTime, byte[] sigA_v, byte[] sigA_r, byte[] sigA_s) {
        this.version = version;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sigA_v = sigA_v;
        this.sigA_r = sigA_r;
        this.sigA_s = sigA_s;
    }

    public ProofOfIntentRMI(int version, DeXTTAddress sender, DeXTTAddress receiver, BigInteger amount, Date startTime, Date endTime, Sign.SignatureData sigA) {
        this(version, sender.toString(), receiver.toString(), amount, startTime, endTime, sigA.getV(), sigA.getR(), sigA.getS());
    }

    public int getVersion() {
        return version;
    }

    public DeXTTAddress getSender() {
        return new DeXTTAddress(sender);
    }

    public DeXTTAddress getReceiver() {
        return new DeXTTAddress(receiver);
    }

    public BigInteger getAmount() {
        return amount;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public Sign.SignatureData getSigA() {
        return new Sign.SignatureData(this.sigA_v, this.sigA_r, this.sigA_s);
    }

}
