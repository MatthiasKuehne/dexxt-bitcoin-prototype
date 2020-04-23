package DeXTT.Exception;

public class FullClaimMissingException extends Exception {

    public FullClaimMissingException() {
    }

    public FullClaimMissingException(String message) {
        super(message);
    }

    public FullClaimMissingException(String message, Throwable cause) {
        super(message, cause);
    }

    public FullClaimMissingException(Throwable cause) {
        super(cause);
    }

    public FullClaimMissingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
