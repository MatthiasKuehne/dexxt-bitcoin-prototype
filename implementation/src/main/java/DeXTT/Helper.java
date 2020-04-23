package DeXTT;

import DeXTT.DataStructure.ProofOfIntentData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Pattern;

import static Configuration.Constants.DEXTT_KEYWORD_BYTES;
import static Configuration.Constants.SUPPORTED_VERSION;

public class Helper {

    private static final Logger logger = LogManager.getLogger();

    private static Pattern hexDigitsPattern = Pattern.compile("^[0-9A-Fa-f]+$");

    /**
     * Converts a String representation of hex-encoded characters to a byte array
     *
     * from https://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java/140861#140861
     * @param s the hex String
     *          must be a multiple of 2
     * @return  the byte array
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static boolean isHexString(String s) {
        return hexDigitsPattern.matcher(s).matches();
    }

    /**
     * puts "DeXTT" keyword into the beginning of the payload
     * @param payload   the DeXTT payload
     */
    public static void putDeXTTKeywordAndVersionToPayload(byte[] payload) {
        if (payload.length >= (DEXTT_KEYWORD_BYTES.length + 1)) { // enough space for keyword + version byte
            for (int i = 0; i < DEXTT_KEYWORD_BYTES.length; i++) {
                payload[i] = DEXTT_KEYWORD_BYTES[i];
            }
            payload[DEXTT_KEYWORD_BYTES.length] = (byte) SUPPORTED_VERSION;
        }
    }

    public static void putArrayIntoPayload(byte[] payload, byte[] array, int startIndex, int endIndex) {
        if ((endIndex - startIndex >= 0) && (endIndex - startIndex) <= array.length && payload.length >= endIndex) {
            System.arraycopy(array, 0, payload, startIndex, endIndex - startIndex);
        }
    }

    public static void putAmountIntoPayload(byte[] payload, BigInteger amount, int startIndex, int amountLength) {
        if (payload.length >= (startIndex + amountLength)) {
            byte[] amountBytes = amount.toByteArray();
            if (amountBytes.length == amountLength + 1 && amountBytes[0] == 0x00) {
                // + 1 byte to force unsigned, remove byte
                amountBytes = Arrays.copyOfRange(amountBytes, 1, amountBytes.length);
            }
            if (amountBytes.length <= amountLength) {
                int lenZeroBytes = amountLength - amountBytes.length;
                int endIndex = startIndex + lenZeroBytes;
                Arrays.fill(payload, startIndex, endIndex, (byte) 0x00);
                startIndex = endIndex;
                endIndex = startIndex + amountBytes.length;
                putArrayIntoPayload(payload, amountBytes, startIndex, endIndex);
            } else {
                logger.error("Amount too high for DeXTT transactions!");
            }
        }
    }

    public static void putDateIntoPayload(byte[] payload, Date date, int startIndex, int dateLength) {
        if (payload.length >= (startIndex + dateLength)) {
            long seconds = date.getTime() / 1000L;
            String hexStringSeconds = Long.toHexString(seconds);
            if (hexStringSeconds.length() % 2 != 0) {
                // append zero
                hexStringSeconds = "0" + hexStringSeconds;
            }
            byte[] secondsBytes = Helper.hexStringToByteArray(hexStringSeconds);

            if (secondsBytes.length <= dateLength) {
                int lenZeroBytes = dateLength - secondsBytes.length;
                int endIndex = startIndex + lenZeroBytes;
                Arrays.fill(payload, startIndex, endIndex, (byte) 0x00);
                startIndex = endIndex;
                endIndex = startIndex + secondsBytes.length;
                putArrayIntoPayload(payload, secondsBytes, startIndex, endIndex);
            }
        }
    }

    public static void putSignatureIntoPayload(byte[] payload, Sign.SignatureData signature, int startIndex) {
        if (payload.length >= (startIndex + signature.getV().length + signature.getR().length + signature.getS().length)) {
            System.arraycopy(signature.getV(), 0, payload, startIndex, signature.getV().length);
            startIndex += signature.getV().length;
            System.arraycopy(signature.getR(), 0, payload, startIndex, signature.getR().length);
            startIndex += signature.getR().length;
            System.arraycopy(signature.getS(), 0, payload, startIndex, signature.getS().length);
        }
    }

    public static Date calculateVetoEndTime(ProofOfIntentData originalPoi, ProofOfIntentData newPoi) {
        Date maxT1 = originalPoi.getEndTime();
        if (newPoi.getEndTime().after(maxT1)) {
            maxT1 = newPoi.getEndTime();
        }

        Date vetoEndTime = new Date(maxT1.getTime()
                + (originalPoi.getEndTime().getTime() - originalPoi.getStartTime().getTime())
                + (newPoi.getEndTime().getTime() - newPoi.getStartTime().getTime()));
        return vetoEndTime;
    }
}
