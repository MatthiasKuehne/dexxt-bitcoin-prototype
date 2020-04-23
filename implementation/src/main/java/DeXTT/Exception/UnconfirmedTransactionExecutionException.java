package DeXTT.Exception;

public class UnconfirmedTransactionExecutionException extends Exception {
    public UnconfirmedTransactionExecutionException() {
    }

    public UnconfirmedTransactionExecutionException(String message) {
        super(message);
    }

    public UnconfirmedTransactionExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnconfirmedTransactionExecutionException(Throwable cause) {
        super(cause);
    }

    public UnconfirmedTransactionExecutionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
