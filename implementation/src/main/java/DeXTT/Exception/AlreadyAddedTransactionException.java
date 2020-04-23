package DeXTT.Exception;

public class AlreadyAddedTransactionException extends Exception {

    public AlreadyAddedTransactionException() {
    }

    public AlreadyAddedTransactionException(String message) {
        super(message);
    }

    public AlreadyAddedTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlreadyAddedTransactionException(Throwable cause) {
        super(cause);
    }

    public AlreadyAddedTransactionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
