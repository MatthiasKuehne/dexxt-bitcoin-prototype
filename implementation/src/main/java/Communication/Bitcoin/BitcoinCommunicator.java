package Communication.Bitcoin;

import Configuration.Configuration;
import DeXTT.Helper;
import DeXTT.Transaction.Bitcoin.RawBitcoinTransaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.web3j.utils.Numeric;
import wf.bitcoin.javabitcoindrpcclient.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static Configuration.Constants.*;

public class BitcoinCommunicator {

    private static final Logger logger = LogManager.getLogger();
    private Configuration configuration = Configuration.getInstance();
    private BitcoinJSONRPCClient bitcoin;
    private String url;

    public BitcoinCommunicator(String urlRPC) throws MalformedURLException {
        this.url = urlRPC;
        URL url = new URL(urlRPC);
        bitcoin = new BitcoinJSONRPCClient(url);
    }

    public String getPrivateKey() throws GenericRpcException {
        String walletPassphrase = configuration.getWalletPassphrase();
        if (walletPassphrase != null) {
            bitcoin.walletPassPhrase(walletPassphrase, 10);
        }
        return bitcoin.dumpPrivKey(configuration.getBitcoinAddress());
    }

    /**
     * Needs to be synchronized, otherwise possible to use same UTXO multiple times
     * @param payload
     * @return
     * @throws GenericRpcException
     */
    public synchronized long sendDeXTTTransaction(byte[] payload) throws GenericRpcException {
        if (payload == null || payload.length == 0 || payload.length > 80) {
            logger.error("Invalid DeXTT payload!");
            return -1;
        }

        BigDecimal feeRate = getFeeRate();
        BigDecimal payloadLenMultiplier = new BigDecimal(payload.length).multiply(new BigDecimal("0.001"));

        BitcoindRpcClient.SignedRawTransaction signedRawTransaction = null;
        BitcoindRpcClient.RawTransaction signedTransaction = null;
        BigDecimal txSizeMultiplier = SIZE_ESTIMATE_MULTIPLIER.add(payloadLenMultiplier); // add payload size to estimate
        boolean feeOK = false;
        while (!feeOK) {
            BigDecimal fee = feeRate.multiply(txSizeMultiplier).setScale(8, RoundingMode.CEILING);
            if (fee.compareTo(MINIMUM_FEE) < 0) {
                fee = MINIMUM_FEE;
            }
            BitcoindRpcClient.Unspent unspentInput = getInputTX(fee);

            if (unspentInput == null) {
                logger.error("No UTXO to spend on fee!");
                return -1;
            }

            // input -> UTXO with high enough amount
            BitcoindRpcClient.TxInput input = new BitcoindRpcClient.ExtendedTxInput(unspentInput.txid(), unspentInput.vout(), unspentInput.scriptPubKey(), unspentInput.amount(), unspentInput.redeemScript(), unspentInput.witnessScript());

            BigDecimal outAmount = unspentInput.amount().subtract(fee); // change
            BitcoindRpcClient.TxOutput output = new BitcoindRpcClient.BasicTxOutput(configuration.getBitcoinAddress(), outAmount, payload);

            String rawTransaction = bitcoin.createRawTransaction(Arrays.asList(input), Arrays.asList(output));

            String key = getPrivateKey();
            signedRawTransaction = bitcoin.signRawTransactionWithKey(rawTransaction, Arrays.asList(key), null, SignatureHashType.ALL);
            signedTransaction = bitcoin.decodeRawTransaction(signedRawTransaction.hex());

            // check if size estimate was ok
            BigDecimal actualSizeMultiplier = new BigDecimal(signedTransaction.vsize()).multiply(new BigDecimal("0.001"));
            if ((actualSizeMultiplier.subtract(txSizeMultiplier)).abs().compareTo(new BigDecimal("0.002")) < 1) {
                // |actual - estimate| <= 2 vbytes
                feeOK = true;
                logger.debug("Fee estimate was ok, sending transaction.");
            } else {
                // not ok, correct fee
                txSizeMultiplier = actualSizeMultiplier;
                logger.debug("Fee estimate was off, redoing transaction.");
            }
        }

        bitcoin.sendRawTransaction(signedRawTransaction.hex());
        logger.info("[" + this.url + "] DeXTT transaction sent successfully: txid = " + signedTransaction.txId() + ", payload = " + Numeric.toHexStringNoPrefix(payload));
        return signedTransaction.vsize();
    }

    public List<RawBitcoinTransaction> filterBlocksForDeXTT(int firstBlockHeight, int lastBlockHeight) throws GenericRpcException {
        List<RawBitcoinTransaction> rawTransactions = new ArrayList<>();
        for (int height = firstBlockHeight; height <= lastBlockHeight; height++) {
            BitcoindRpcClient.Block block = bitcoin.getBlock(height);
            for (String txid : block.tx()) {
                RawBitcoinTransaction raw = this.filterTxIdForDeXTT(txid);
                if (raw != null) {
                    raw.setTime(block.time());
                    rawTransactions.add(raw);
                }
            }
        }
        return rawTransactions;
    }

    /**
     *
     * @param txIdsAlreadyFound
     * @return
     */
    public Set<String> getNewTxIdsSinceLastBlock(Set<String> txIdsAlreadyFound) throws GenericRpcException {
        Set<String> newTxIds = new LinkedHashSet<>();

        int blockCount = this.getBlockCount();
        String blockHash = this.bitcoin.getBlockHash(blockCount);
        BitcoindRpcClient.TransactionsSinceBlock txs = this.bitcoin.listSinceBlock(blockHash);
        for (BitcoindRpcClient.Transaction tx: txs.transactions()) {
            if (txIdsAlreadyFound == null || !txIdsAlreadyFound.contains(tx.txId())) {
                // only include ones not found yet
                newTxIds.add(tx.txId());
            }
        }

        return newTxIds;
    }

    /**
     *
     * @param txId
     * @return  null if tx does not contain a DeXTT tx
     */
    public RawBitcoinTransaction filterTxIdForDeXTT(String txId) throws GenericRpcException {
        RawBitcoinTransaction raw = null;
        BitcoindRpcClient.RawTransaction rawTransaction;
        try {
            rawTransaction = bitcoin.getRawTransaction(txId);
        } catch (BitcoinRPCException e) {
            if (e.getResponse().contains("\"code\":-5,\"message\":\"No such mempool or blockchain transaction. Use gettransaction for wallet transactions.\"")) {
                return null;
            } else {
                throw e;
            }
        }
        for (BitcoindRpcClient.RawTransaction.Out out : rawTransaction.vOut()) {
            String asm = out.scriptPubKey().asm();
            if (out.scriptPubKey().type().equals("nulldata") && asm.startsWith("OP_RETURN ") && asm.length() >= 20) { // "OP_RETURN " -> 10, DeXTT as hex string -> 10 chars
                // found OP_RETURN, look for DeXTT keyword

                // index [0, 9]: "OP_RETURN ", index [10, 19] should be hex representation of "DeXTT"
                String keywordAscii = new String(Helper.hexStringToByteArray(out.scriptPubKey().asm().substring(10, 20)));
                if (keywordAscii.equals(DEXTT_KEYWORD)) {
                    logger.debug("Found DeXTT tx: txid: " + txId + ", data: " + out.scriptPubKey().asm());

                    // get public key of TX-sender from unlocked input
                    // works for number 0 witness programs
                    String publicKeyHex = null;
                    for (BitcoindRpcClient.RawTransaction.In in : rawTransaction.vIn()) {
                        String txIn = in.toString();
                        int witnessIndex = txIn.indexOf("txinwitness");
                        int startPubKey = txIn.indexOf(", ", witnessIndex) + 2;
                        int closingBracket = txIn.indexOf("]", startPubKey);
                        if (witnessIndex >= 0 && startPubKey >= 0 && startPubKey < txIn.length() && closingBracket >= 0 && closingBracket < txIn.length()) {
                            String key = txIn.substring(startPubKey, closingBracket);
                            if (key.length() == 66) { // 33 bytes (-> 66 hex chars) for compressed public key
                                logger.debug("Found public key: " + key);
                                publicKeyHex = key;
                                break;
                            }
                        }
                    }
                    if (publicKeyHex != null) {
                        String payload = asm.substring(20);
                        if (payload.length() > 0) {
                            raw = new RawBitcoinTransaction(publicKeyHex, Helper.hexStringToByteArray(payload), rawTransaction.txId(), Optional.ofNullable(rawTransaction.confirmations()).orElse(0));
                        }
                    } else {
                        logger.debug("Could not retrieve public key from TX inputs, ignoring DeXTT TX.");
                    }
                }
            }
        }

        return raw;
    }

    public int getBlockCount() throws GenericRpcException {
        return this.bitcoin.getBlockCount();
    }

    public Date getBlockTime(int blockHeight) throws GenericRpcException {
        return this.bitcoin.getBlock(blockHeight).time();
    }

    public void generateToAddress(String bitcoinAddress) throws GenericRpcException {
        this.bitcoin.generateToAddress(1, bitcoinAddress);
    }

    public String getUrl() {
        return url;
    }

    /**
     * Feerate estimate for inclusion in next block
     * @return
     * @throws GenericRpcException
     */
    private BigDecimal getFeeRate() throws GenericRpcException {
        BigDecimal rate;
        BitcoindRpcClient.SmartFeeResult feeResult = bitcoin.estimateSmartFee(1);
        rate = feeResult.feeRate();
        return Objects.requireNonNullElse(rate, DEFAULT_FEE);
    }

    private BitcoindRpcClient.Unspent getInputTX(BigDecimal fee) throws GenericRpcException {
        // first try to use only confirmed UTXOs
        BitcoindRpcClient.Unspent unspentInput = this.getInputTX(fee, 1, 99999999);

        // if no confirmed UTXO found, search for an unconfirmed UTXO
        if (unspentInput == null) {
            unspentInput = this.getInputTX(fee, 0, 0);
        }

        return unspentInput;
    }

    private BitcoindRpcClient.Unspent getInputTX(BigDecimal fee, int minConf, int maxConf) {
        List<BitcoindRpcClient.Unspent> unspentTXs = bitcoin.listUnspent(minConf, maxConf, configuration.getBitcoinAddress());

        BitcoindRpcClient.Unspent unspentInput = null;
        for (BitcoindRpcClient.Unspent unspent: unspentTXs) {
            if (unspent.amount().compareTo(fee) >= 0) {
                unspentInput = unspent;
                break;
            }
        }
        return unspentInput;
    }
}
