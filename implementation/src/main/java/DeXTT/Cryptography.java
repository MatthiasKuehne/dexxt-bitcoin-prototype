package DeXTT;

import Configuration.Configuration;
import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.DataStructure.ProofOfIntentData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;

public class Cryptography {

    private static final Logger logger = LogManager.getLogger();

    private static Configuration configuration = Configuration.getInstance();

    /**
     * Creates a DeXTT/Ethereum address from a given public key in hex format
     * @param publicKeyHex  the public key in hex format in either:
     *                      uncompressed form without 0x04 prefix (128 hex-digits String/ 64 bytes key)
     *                      uncompressed form with 0x04 prefix (130 hex-digits String / 65 bytes key)
     *                      compressed form (66 hex-digits String / 33 bytes key)
     * @return  the DeXTT/Ethereum address corresponding to the given public key
     *          null    if unsupported key length
     */
    public static DeXTTAddress createAddressFromPublicKeyHex(String publicKeyHex) {
        if (!Helper.isHexString(publicKeyHex)) {
            // not a hex string
            return null;
        }
        if (publicKeyHex.length() == 130) {
            // uncompressed with prefix -> remove prefix
            publicKeyHex = publicKeyHex.substring(2);
        } else if (publicKeyHex.length() == 66) {
            // compressed public key -> decompress
            ECKey key = ECKey.fromPublicOnly(Numeric.hexStringToByteArray(publicKeyHex));
            key = key.decompress(); // with prefix
            publicKeyHex = key.getPublicKeyAsHex().substring(2);
        } else if (publicKeyHex.length() != 128) {
            // no supported public key format
            return null;
        }

        String address = Keys.getAddress(publicKeyHex);
        return new DeXTTAddress(address);
    }

    /**
     *
     * @param privateKeyBase58
     * @return
     */
    public static DeXTTAddress createAddressFromWIFPrivateKey(String privateKeyBase58) {
        DumpedPrivateKey dumpedPrivateKey = DumpedPrivateKey.fromBase58(configuration.getNetworkParameters(), privateKeyBase58);
        ECKey key = dumpedPrivateKey.getKey();

        return createAddressFromPublicKeyHex(key.getPublicKeyAsHex());
    }

    public static ECKeyPair createSigningKeyFromWIFPrivateKey(String privateKeyBase58) {
        DumpedPrivateKey dumpedPrivateKey = DumpedPrivateKey.fromBase58(configuration.getNetworkParameters(), privateKeyBase58);
        ECKey key = dumpedPrivateKey.getKey();
        BigInteger privateKey = new BigInteger(key.getPrivateKeyAsHex(),16);
        BigInteger publicKey = Sign.publicKeyFromPrivate(privateKey); // uncompressed public key w/o leading 0x04
        return new ECKeyPair(privateKey, publicKey);
    }

    /**
     * returns the last 8 bytes of the full poi hash
     * @param poi
     * @return  the last 8 bytes of the full poi hash
     */
    public static BigInteger calculateShortPoiHash(ProofOfIntentData poi) {
        byte[] poiHash = new byte[8];
        byte[] fullHash = alphaData(poi);

        System.arraycopy(fullHash, fullHash.length - 8, poiHash, 0, 8);

        return new BigInteger(1, poiHash);
    }

    public static boolean verifyShortPoiHash(ProofOfIntentData poi, BigInteger poiHashShort) {
        BigInteger correctPoiHashShort = calculateShortPoiHash(poi);
        return correctPoiHashShort.equals(poiHashShort);
    }

    public static BigInteger calculateFullPoiHash(ProofOfIntentData poi) {
        byte[] fullHash = alphaData(poi);
        return new BigInteger(1, fullHash);
    }

    public static boolean verifyFullPoiHash(ProofOfIntentData poi, BigInteger poiHashFull) {
        BigInteger correctPoiHashFull = calculateFullPoiHash(poi);
        return correctPoiHashFull.equals(poiHashFull);
    }

    /**
     * returns hash of PoI
     * @param poi
     * @return
     */
    public static byte[] alphaData(ProofOfIntentData poi) {
        byte[] toHash = encodePacked("a", poi);
        return Hash.sha3(toHash);
    }

    /**
     * hash of sigA
     * @param sigA
     * @return
     */
    public static byte[] betaData(Sign.SignatureData sigA) {
        byte[] toHash = encodePacked("b", sigA);
        return Hash.sha3(toHash);
    }

    public static Sign.SignatureData signPoi(ProofOfIntentData poi, ECKeyPair keyPair) {
        return Sign.signMessage(alphaData(poi), keyPair, false);
    }

    public static Sign.SignatureData signAlphaData(Sign.SignatureData sigA, ECKeyPair keyPair) {
        return Sign.signMessage(betaData(sigA), keyPair, false);
    }

    public static boolean verifySigA(ProofOfIntentData poi, Sign.SignatureData signature) {
        return verifySignature(alphaData(poi), poi.getSender(), signature);
    }

    public static boolean verifySigB(Sign.SignatureData sigA, DeXTTAddress receiver, Sign.SignatureData sigB) {
        return verifySignature(betaData(sigA), receiver, sigB);
    }

    /**
     *
     * @param data must be already a 32 byte hash
     * @param from
     * @param signature
     * @return
     */
    private static boolean verifySignature(byte[] data, DeXTTAddress from, Sign.SignatureData signature) {
        try {
            BigInteger recoveredKey = Sign.signedMessageHashToKey(data, signature);
            String hex = Numeric.toHexStringNoPrefixZeroPadded(recoveredKey, 128); // 128 hex digits / 64 bytes
            DeXTTAddress recoveredAddress = createAddressFromPublicKeyHex(hex);

            return from.equals(recoveredAddress);

        } catch (SignatureException e) {
            return false;
        }
    }

    public static BigInteger contestantSignature(DeXTTAddress contestant, BigInteger alphaData) {
        byte[] toHash = encodePacked(contestant, alphaData);
        byte[] hash = Hash.sha3(toHash);
        return new BigInteger(1, hash);
    }

    public static BigInteger vetoSignature(DeXTTAddress contestant, DeXTTAddress conflictingSender) {
        byte[] toHash = encodePacked("v", contestant, conflictingSender);
        byte[] hash = Hash.sha3(toHash);
        return new BigInteger(1, hash);
    }

    private static byte[] encodePacked(String prefix, ProofOfIntentData poi) {
        byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] encoded = new byte[prefixBytes.length + (32 * 5)]; // 2 byte for each character + 5 times a 32 byte value (2x address, amount, 2x dates)

        int index;
        for (index = 0; index < prefixBytes.length; index++) {
            encoded[index] = prefixBytes[index];
        }

        byte[] sender = poi.getSender().getAddressBytesWithoutPrefix();
        System.arraycopy(sender, 0, encoded, index, sender.length);
        index += sender.length;

        byte[] receiver = poi.getReceiver().getAddressBytesWithoutPrefix();
        System.arraycopy(receiver, 0, encoded, index, receiver.length);
        index += receiver.length;

        Helper.putAmountIntoPayload(encoded, poi.getAmount(), index, 32);
        index += 32;

        Helper.putDateIntoPayload(encoded, poi.getStartTime(), index, 32);
        index += 32;

        Helper.putDateIntoPayload(encoded, poi.getEndTime(), index, 32);

        return encoded;
    }

    private static byte[] encodePacked(String prefix, Sign.SignatureData sigA) {
        byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] encoded = new byte[prefixBytes.length + 65]; // 2 byte for each character + 65 byte sigA

        int index;
        for (index = 0; index < prefixBytes.length; index++) {
            encoded[index] = prefixBytes[index];
        }

        Helper.putSignatureIntoPayload(encoded, sigA, index);

        return encoded;
    }

    private static byte[] encodePacked(DeXTTAddress address, BigInteger alphaData) {
        byte[] alphaDataBytes = new byte[32]; // is 32 byte hash
        Helper.putAmountIntoPayload(alphaDataBytes, alphaData, 0, 32);


        return encodePacked("", address.getAddressBytesWithoutPrefix(), alphaDataBytes);
    }

    private static byte[] encodePacked(String prefix, DeXTTAddress address1, DeXTTAddress address2) {
        return encodePacked(prefix, address1.getAddressBytesWithoutPrefix(), address2.getAddressBytesWithoutPrefix());
    }

    private static byte[] encodePacked(String prefix, byte[] array1, byte[] array2) {
        byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] encoded = new byte[prefixBytes.length + array1.length + array2.length];

        int index;
        for (index = 0; index < prefixBytes.length; index++) {
            encoded[index] = prefixBytes[index];
        }

        System.arraycopy(array1, 0, encoded, index, array1.length);
        index += array1.length;

        System.arraycopy(array2, 0, encoded, index, array2.length);

        return encoded;
    }
}
