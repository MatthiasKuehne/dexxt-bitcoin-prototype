package DeXTT.Transaction.Bitcoin;

import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.Helper;
import DeXTT.Transaction.MintTransaction;
import DeXTT.Transaction.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.List;

import static Configuration.Constants.*;

public class BitcoinMintTransaction implements BitcoinTransaction {

    private static final Logger logger = LogManager.getLogger();

    private DeXTTAddress receiver;

    private BigInteger amount;

    private DeXTTAddress bitcoinTransactionSender;

    public BitcoinMintTransaction(DeXTTAddress receiver, BigInteger amount) {
        this.receiver = receiver;
        this.amount = amount;
    }

    public BitcoinMintTransaction(DeXTTAddress receiver, BigInteger amount, DeXTTAddress bitcoinTransactionSender) {
        this(receiver, amount);
        this.bitcoinTransactionSender = bitcoinTransactionSender;
    }

    @Override
    public List<Transaction> putIntoDeXTTTransaction(List<Transaction> transaction) {
        return null;
    }

    @Override
    public Transaction createNewCorrespondingDeXTTTransaction() {
        return new MintTransaction(this.receiver, this.amount, this.bitcoinTransactionSender);
    }

    @Override
    public byte[] convertToDeXTTPayload() {
        byte[] payload = new byte[(DEXTT_KEYWORD_BYTES.length + MINT_TRANSACTION_LENGTH)];

        Helper.putDeXTTKeywordAndVersionToPayload(payload);
        payload[DEXTT_KEYWORD_BYTES.length + 1] = (byte) MINT_TRANSACTION_TYPE;

        byte[] address = this.receiver.getAddressBytesWithoutPrefix();
        int startIndex = DEXTT_KEYWORD_BYTES.length + 2;
        int endIndex = startIndex + address.length;
        Helper.putArrayIntoPayload(payload, address, startIndex, endIndex);
        Helper.putAmountIntoPayload(payload, this.amount, endIndex, 16);

        return payload;
    }
}