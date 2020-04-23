package Events;

import DeXTT.Transaction.ContestTransaction;

public class UnconfirmedContestReferenceTransactionEvent {

    private ContestTransaction contestTransaction;

    public UnconfirmedContestReferenceTransactionEvent(ContestTransaction contestTransaction) {
        this.contestTransaction = contestTransaction;
    }

    public ContestTransaction getContestTransaction() {
        return contestTransaction;
    }
}
