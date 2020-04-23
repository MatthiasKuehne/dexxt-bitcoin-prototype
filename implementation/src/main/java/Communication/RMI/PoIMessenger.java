package Communication.RMI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PoIMessenger implements PoIMessengerInterface {

    private static final Logger logger = LogManager.getLogger();

    private RMIProvider provider;

    public PoIMessenger(RMIProvider provider) {
        this.provider = provider;
    }

    @Override
    public void registerPoI(ProofOfIntentRMI poi) {
        logger.debug("Received poi: " + poi);
        this.provider.addPoI(poi); // already thread safe
    }
}
