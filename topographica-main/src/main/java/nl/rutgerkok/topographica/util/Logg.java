package nl.rutgerkok.topographica.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Simple class for logging warnings, which can be shown to the server admin at
 * a later moment.
 *
 */
public class Logg {

    private final List<String> messages = new ArrayList<>();
    private final Logger logger;

    public Logg(Logger logger) {
        this.logger = logger;
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
