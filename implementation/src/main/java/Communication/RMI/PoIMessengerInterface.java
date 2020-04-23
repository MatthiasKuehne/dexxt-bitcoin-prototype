package Communication.RMI;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PoIMessengerInterface extends Remote {

    void registerPoI(ProofOfIntentRMI poi) throws RemoteException;

}
