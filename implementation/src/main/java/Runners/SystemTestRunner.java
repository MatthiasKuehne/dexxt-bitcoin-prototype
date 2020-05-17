package Runners;

import Communication.RMI.PoIMessengerInterface;
import Communication.RMI.ProofOfIntentRMI;
import Communication.RMI.RMIProvider;
import Configuration.Configuration;
import DeXTT.ClientsService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wf.bitcoin.javabitcoindrpcclient.GenericRpcException;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import static java.lang.System.exit;

// only used for testing without whole evaluationrunner logic
public class SystemTestRunner {

    private static final Logger logger = LogManager.getLogger();

    private Configuration configuration;

    private ClientsService clientsService;

    private long dexxtTransactionTimeSeconds;

    private boolean forceVetoTransactions;

    public SystemTestRunner(long dexxtTransactionTimeSeconds, boolean forceVetoTransactions) {
        this.configuration = Configuration.getInstance();
        this.dexxtTransactionTimeSeconds = dexxtTransactionTimeSeconds;
        this.forceVetoTransactions = forceVetoTransactions;
        try {
            this.clientsService = new ClientsService();
        } catch (AlreadyBoundException | RemoteException | GenericRpcException | MalformedURLException e) {
            logger.fatal("Error occurred: " + e.getMessage());
            exit(1);
        }
    }

    public void execute() {
        this.clientsService.initialize(); // not really needed to initialize here, but needs earliest blocks for t0 of TX

        int numClaims = 1;
        if (forceVetoTransactions) {
            numClaims++;
        }
        for (int i = 0; i < numClaims; i++) {
            ProofOfIntentRMI poi = this.clientsService.createRandomPoi(this.dexxtTransactionTimeSeconds);
            if (poi != null) {
                try {
                    PoIMessengerInterface poIMessengerInterface = RMIProvider.getRemoteObjectStub(poi.getReceiver());
                    poIMessengerInterface.registerPoI(poi);
                } catch (RemoteException | NotBoundException e) {
                    e.printStackTrace();
                }
            }
        }
        this.clientsService.close();
    }
}
