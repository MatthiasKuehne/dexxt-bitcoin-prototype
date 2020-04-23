package Events;

import java.util.Date;

public class NewBlockFoundEvent {

    String rpcUrl;

    Date blockTime;

    public NewBlockFoundEvent(String rpcUrl, Date blockTime) {
        this.rpcUrl = rpcUrl;
        this.blockTime = blockTime;
    }

    public String getRpcUrl() {
        return rpcUrl;
    }

    public Date getBlockTime() {
        return blockTime;
    }
}
