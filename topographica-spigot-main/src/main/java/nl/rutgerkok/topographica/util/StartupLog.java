package nl.rutgerkok.topographica.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Simple class for logging warnings, which can be shown to the server admin at
 * a later moment.
 *
 */
public final class StartupLog {

    private final List<String> messages = new ArrayList<>();
    private final Logger logger;

    public StartupLog(Logger logger) {
        this.logger = logger;
    }

    /**
     * Gets all messages displayed up until now.
     * 
     * @return All messages.
     */
    public List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void severe(String message) {
        messages.add(message);
        logger.severe(message);
    }

    public void warn(String message) {
        messages.add(message);
        logger.warning(message);
    }
}
