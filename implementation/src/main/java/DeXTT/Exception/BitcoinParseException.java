package DeXTT.Exception;

public class BitcoinParseException extends Exception {
    public BitcoinParseException() {
    }

    public BitcoinParseException(String message) {
        super(message);
    }

    public BitcoinParseException(Throwable cause) {
        super(cause);
    }

    public BitcoinParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public BitcoinParseException(String message, Throwable cause, boolean enableSuppression, boolean writeableStackTrace) {
        super(message, cause, enableSuppression, writeableStackTrace);
    }
}
