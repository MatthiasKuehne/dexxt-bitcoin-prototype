package Events;

import com.google.common.eventbus.EventBus;

//Singleton
// application-wide events
public class GlobalEventBus {

    private static GlobalEventBus instance;

    private EventBus eventBus;

    private GlobalEventBus() {
        this.eventBus = new EventBus();
    }

    public static synchronized GlobalEventBus getInstance() {
        if (instance == null) {
            instance = new GlobalEventBus();
        }
        return instance;
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
