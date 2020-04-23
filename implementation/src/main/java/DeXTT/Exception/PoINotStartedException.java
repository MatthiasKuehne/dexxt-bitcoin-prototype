package DeXTT.Exception;

public class PoINotStartedException extends Exception {
    public PoINotStartedException() {
        super();
    }

    public PoINotStartedException(String message) {
        super(message);
    }

    public PoINotStartedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PoINotStartedException(Throwable cause) {
        super(cause);
    }

    protected PoINotStartedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
