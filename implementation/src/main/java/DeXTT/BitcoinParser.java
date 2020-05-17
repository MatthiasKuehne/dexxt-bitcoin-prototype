package DeXTT;

import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.DataStructure.ProofOfIntentData;
import DeXTT.Exception.BitcoinParseException;
import DeXTT.Exception.UnconfirmedTransactionExecutionException;
import DeXTT.Transaction.Bitcoin.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;

import static Configuration.Constants.*;
import static DeXTT.Cryptography.createAddressFromPublicKeyHex;
import static java.lang.Byte.toUnsignedInt;

public class BitcoinParser {

    private static final Logger logger = LogManager.getLogger();

    public static BitcoinTransaction parseBitcoinDeXTTTransaction(RawBitcoinTransaction rawTransaction)
            throws BitcoinParseException, UnconfirmedTransactionExecutionException {
        byte[] payload = rawTransaction.getPayload();
        if (payload.length < 2) {
            throwException("Payload too short, no version + TX type.");
        }
        byte version = payload[0];

        int versionInteger = toUnsignedInt(version);
        if (versionInteger > SUPPORTED_VERSION) {
            throwException("Unsupported version detected: " + versionInteger);
        }

        byte txType = payload[1];
        int txTypeInteger = toUnsignedInt(txType);

        switch (txTypeInteger) {
            case CLAIM_DATA_TRANSACTION_TYPE:
                return parseClaimDataTransaction(rawTransaction);
            case CLAIM_SIG_TRANSACTION_A_TYPE:
                return parseClaimSigTransactionA(rawTransaction);
            case CLAIM_SIG_TRANSACTION_B_TYPE:
                return parseClaimSigTransactionB(rawTransaction);
            case CONTEST_PARTICIPATION_TRANSACTION_TYPE:
                return parseContestParticipationTransaction(rawTransaction);
            case FINALIZE_TRANSACTION_TYPE:
                if (rawTransaction.getTime() != null && rawTransaction.getConfirmations() > 0) {
                    return parseFinalizeTransaction(rawTransaction);
                } else {
                    throw new UnconfirmedTransactionExecutionException("No BlockTime available, needed for Transaction type!");
                }
            case FINALIZE_VETO_TRANSACTION_TYPE:
                if (rawTransaction.getTime() != null && rawTransaction.getConfirmations() > 0) {
                    return parseFinalizeVetoTransaction(rawTransaction);
                } else {
                    throw new UnconfirmedTransactionExecutionException("No BlockTime available, needed for Transaction type!");
                }
            case MINT_TRANSACTION_TYPE:
                return parseMintTransaction(rawTransaction);
            default:
                logger.debug("Unsupported TX type detected: " + txTypeInteger);
                throw new BitcoinParseException("Unsupported TX type detected: " + txTypeInteger);
        }
    }

    private static BitcoinTransaction parseClaimDataTransaction(RawBitcoinTransaction rawTransaction) throws BitcoinParseException {
        byte[] payload = rawTransaction.getPayload();
        if (payload.length != CLAIM_DATA_TRANSACTION_LENGTH) {
            throwException("Claim Data Transaction with wrong length.");
        }

        DeXTTAddress txSender = new DeXTTAddress(Numeric.toHexString(payload, 2, 20, false));
        DeXTTAddress txReceiver = new DeXTTAddress(Numeric.toHexString(payload, 22, 20, false));
        BigInteger amount = new BigInteger(1, payload, 42, 16);

        // time is stored in seconds (same as in Bitcoin blocks)
        Date startTime = new Date(new BigInteger(1, payload, 58, 4).longValue() * 1000L);
        Date endTime = new Date(new BigInteger(1, payload, 62, 4).longValue() * 1000L);
        BigInteger poiHashShort = new BigInteger(1, payload, 66, 8);

        ProofOfIntentData poi = new ProofOfIntentData(txSender, txReceiver, amount, startTime, endTime);
        if (!Cryptography.verifyShortPoiHash(poi, poiHashShort)) {
            throw new BitcoinParseException("8 Byte PoI hash does not match transaction data, ignoring transaction!");

        } else if (!startTime.before(endTime)) {
            throw new BitcoinParseException("Start time of PoI is not before end time, invalid transaction!");
        } else {
            DeXTTAddress transactionSender = createAddressFromPublicKeyHex(rawTransaction.getPubKeyHexCompressed());
            if (transactionSender == null) {
                throw new BitcoinParseException("Could not create DeXTT Address from public key");
            }
            return new BitcoinClaimDataTransaction(rawTransaction.getTime(), rawTransaction.getConfirmations(), txSender, txReceiver, amount, startTime, endTime, poiHashShort, transactionSender);
        }
    }

    private static BitcoinTransaction parseClaimSigTransactionA(RawBitcoinTransaction rawTransaction) throws BitcoinParseException {
        byte[] payload = rawTransaction.getPayload();
        if (payload.length != CLAIM_SIG_TRANSACTION_A_LENGTH) {
            throwException("Claim Signature Transaction A with wrong length.");
        }

        byte v = payload[2];
        byte[] r = Arrays.copyOfRange(payload, 3, 35);
        byte[] s = Arrays.copyOfRange(payload, 35, 67);
        Sign.SignatureData signature = new Sign.SignatureData(v, r, s);

        BigInteger poiHashShort = new BigInteger(1, payload, 67, 8);
        DeXTTAddress transactionSender = createAddressFromPublicKeyHex(rawTransaction.getPubKeyHexCompressed());
        if (transactionSender == null) {
            throw new BitcoinParseException("Could not create DeXTT Address from public key");
        }
        return new BitcoinClaimSigTransactionA(rawTransaction.getTime(), rawTransaction.getConfirmations(), signature, poiHashShort, transactionSender);
    }

    private static BitcoinTransaction parseClaimSigTransactionB(RawBitcoinTransaction rawTransaction) throws BitcoinParseException {
        byte[] payload = rawTransaction.getPayload();
        if (payload.length != CLAIM_SIG_TRANSACTION_B_LENGTH) {
            throwException("Claim Signature Transaction B with wrong length.");
        }

        byte v = payload[2];
        byte[] r = Arrays.copyOfRange(payload, 3, 35);
        byte[] s = Arrays.copyOfRange(payload, 35, 67);
        Sign.SignatureData signature = new Sign.SignatureData(v, r, s);

        BigInteger poiHashShort = new BigInteger(1, payload, 67, 8);
        DeXTTAddress transactionSender = createAddressFromPublicKeyHex(rawTransaction.getPubKeyHexCompressed());
        if (transactionSender == null) {
            throw new BitcoinParseException("Could not create DeXTT Address from public key");
        }
        return new BitcoinClaimSigTransactionB(rawTransaction.getTime(), rawTransaction.getConfirmations(), signature, poiHashShort, transactionSender);
    }

    private static BitcoinTransaction parseContestParticipationTransaction(RawBitcoinTransaction rawTransaction) throws BitcoinParseException {
        byte[] payload = rawTransaction.getPayload();
        if (payload.length != CONTEST_PARTICIPATION_TRANSACTION_LENGTH) {
            throwException("Contest Participation Transaction with wrong length.");
        }

        BigInteger poiHashFull = new BigInteger(1, payload, 2, 32);
        BigInteger poiHashShort = new BigInteger(1, payload, 26, 8); // last 8 byte of full hash
        DeXTTAddress transactionSender = createAddressFromPublicKeyHex(rawTransaction.getPubKeyHexCompressed());
        if (transactionSender == null) {
            throw new BitcoinParseException("Could not create DeXTT Address from public key");
        }
        return new BitcoinContestTransaction(poiHashFull, poiHashShort, transactionSender, rawTransaction.getTime(), rawTransaction.getConfirmations());
    }

    private static BitcoinTransaction parseFinalizeTransaction(RawBitcoinTransaction rawTransaction) throws BitcoinParseException {
        byte[] payload = rawTransaction.getPayload();
        if (payload.length != FINALIZE_TRANSACTION_LENGTH) {
            throwException("Finalize Transaction with wrong length.");
        }

        BigInteger poiHashFull = new BigInteger(1, payload, 2, 32);
        BigInteger poiHashShort = new BigInteger(1, payload, 26, 8); // last 8 byte of full hash

        return new BitcoinFinalizeTransaction(poiHashFull, poiHashShort, rawTransaction.getTime());
    }

    private static BitcoinTransaction parseFinalizeVetoTransaction(RawBitcoinTransaction rawTransaction) throws BitcoinParseException {
        byte[] payload = rawTransaction.getPayload();
        if (payload.length != FINALIZE_VETO_TRANSACTION_LENGTH) {
            throwException("Finalize-Veto Transaction with wrong length.");
        }

        DeXTTAddress conflictingPoiSender = new DeXTTAddress(Numeric.toHexString(payload, 2, 20, false));

        return new BitcoinFinalizeVetoTransaction(conflictingPoiSender, rawTransaction.getTime());
    }

    private static BitcoinTransaction parseMintTransaction(RawBitcoinTransaction rawTransaction) throws BitcoinParseException {
        byte[] payload = rawTransaction.getPayload();
        if (payload.length != MINT_TRANSACTION_LENGTH) {
            throwException("Mint Transaction with wrong length.");
        }

        DeXTTAddress deXTTAddress = new DeXTTAddress(Numeric.toHexString(payload, 2, 20, false));
        BigInteger amount = new BigInteger(1, payload, 22, 16);

        DeXTTAddress transactionSender = createAddressFromPublicKeyHex(rawTransaction.getPubKeyHexCompressed());
        if (transactionSender == null) {
            throw new BitcoinParseException("Could not create DeXTT Address from public key");
        }
        return new BitcoinMintTransaction(deXTTAddress, amount, transactionSender);
    }

    private static void throwException(String message) throws BitcoinParseException {
        logger.debug(message);
        throw new BitcoinParseException(message);
    }
}
