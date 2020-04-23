package Events;

import DeXTT.Transaction.ClaimTransaction;

public class UnconfirmedClaimContestTransactionEvent {

    private ClaimTransaction claimTransaction;

    public UnconfirmedClaimContestTransactionEvent(ClaimTransaction claimTransaction) {
        this.claimTransaction = claimTransaction;
    }

    public ClaimTransaction getClaimTransaction() {
        return claimTransaction;
    }
}
