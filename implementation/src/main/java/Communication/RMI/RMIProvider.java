package Communication.RMI;

import Configuration.Configuration;
import DeXTT.DataStructure.DeXTTAddress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static Configuration.Constants.RMI_PORT;

public class RMIProvider {

    private static final Logger logger = LogManager.getLogger();
    List<ProofOfIntentRMI> unhandledPoIs;
    PoIMessengerInterface exportedServer;

    public RMIProvider() {
        this.unhandledPoIs = new CopyOnWriteArrayList<>(); // thread safe, also for iterations
    }

    public void addPoI(ProofOfIntentRMI poi) {
        this.unhandledPoIs.add(poi);
    }

    /**
     * Returns all unhandled PoIs and removes them from the list
     * @return all currently unhandled PoIs
     */
    public List<ProofOfIntentRMI> removeUnhandledPoIs() {
        List<ProofOfIntentRMI> returnList = new ArrayList<>();

        List<ProofOfIntentRMI> unhandled = this.unhandledPoIs;
        for (ProofOfIntentRMI poi: unhandled) {
            returnList.add(poi);
            unhandled.remove(poi);
        }
        return returnList;
    }

    /**
     *
     * @param deXTTAddress
     * @throws RemoteException
     * @throws AlreadyBoundException
     */
    public void startRMIServer(DeXTTAddress deXTTAddress) throws RemoteException, AlreadyBoundException {
        PoIMessengerInterface server = new PoIMessenger(this);
        this.exportedServer = server;
        PoIMessengerInterface skeleton = (PoIMessengerInterface) UnicastRemoteObject.exportObject(server, 0);
        Registry registry = RMIProvider.getRegistry();
        registry.bind(bindingName(deXTTAddress), skeleton);
    }

    public void stopRMIServer(DeXTTAddress deXTTAddress) throws RemoteException, NotBoundException {
        Registry registry = RMIProvider.getRegistry();
        registry.unbind(bindingName(deXTTAddress));
        UnicastRemoteObject.unexportObject(this.exportedServer, true);
    }

    /**
     *
     * @param deXTTAddress
     * @return
     * @throws RemoteException
     * @throws NotBoundException
     */
    public static PoIMessengerInterface getRemoteObjectStub(DeXTTAddress deXTTAddress) throws RemoteException, NotBoundException {
        Registry registry = RMIProvider.getRegistry();
        PoIMessengerInterface stub = (PoIMessengerInterface) registry.lookup(bindingName(deXTTAddress));
        return stub;
    }

    private static Registry getRegistry() throws RemoteException {
        Registry registry;
        try {
            registry = LocateRegistry.getRegistry(RMI_PORT);
            logger.debug("Registry found: " + registry.toString());
            logger.debug("Registry list: " + Arrays.toString(registry.list())); // call to registry.list() NEEDED, throws RemoteException if communication not possible
        } catch (RemoteException e) {
            registry = LocateRegistry.createRegistry(RMI_PORT);
            logger.debug("Registry created: " + registry.toString());
        }
        return registry;
    }

    private static String bindingName(DeXTTAddress deXTTAddress) {
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append("PoIMessenger-");
        for (String url: Configuration.getInstance().getUrlRPC()) {
            nameBuilder.append(url).append("-");
        }
        nameBuilder.append(deXTTAddress.toString()); // includes 0x prefix

        return nameBuilder.toString();
    }
}
