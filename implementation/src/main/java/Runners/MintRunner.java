package Runners;

import Configuration.Configuration;
import DeXTT.DataStructure.DeXTTAddress;
import DeXTT.Client;
import DeXTT.Transaction.MintTransaction;
import DeXTT.Transaction.Transaction;
import DeXTT.Helper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wf.bitcoin.javabitcoindrpcclient.GenericRpcException;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class MintRunner {

    private static final Logger logger = LogManager.getLogger();

    private Configuration configuration;

    public MintRunner() {
        this.configuration = Configuration.getInstance();
    }

    /**
     *
     * @param addresses
     * @param amount
     * @return  Error code
     * @throws MalformedURLException
     * @throws GenericRpcException
     */
    public int execute(List<String> addresses, BigInteger amount) throws MalformedURLException, GenericRpcException {
        List<Client> clients = new ArrayList<>();
        for (String url: configuration.getUrlRPC()) {
            Client client = new Client(url);
            clients.add(client);
        }
        for (String address: addresses) {
            if (address.length() != 40) {
                logger.error("Wrong address length: " + address);
                return -1;
            } else if (!Helper.isHexString(address)) {
                logger.error("Address not in hex format: " + address);
                return -1;
            }
        }

        for (String address: addresses) {
            DeXTTAddress addressConverted = new DeXTTAddress(address);
            Transaction mintTransaction = new MintTransaction(addressConverted, amount);
            for (Client client : clients) {
                client.sendDeXTTTransaction(mintTransaction);
            }
        }

        return 0;
    }
}
