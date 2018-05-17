package nl.rutgerkok.topographica.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Simple class for logging warnings, which can be shown to the server admin at
 * a later moment.
 *
 */
public abstract class StartupLog {

    private static class NullLogger extends StartupLog {

        @Override
        public List<String> getMessages() {
            return Collections.emptyList();
        }

        @Override
        public void severe(String message) {
            // Do nothing
        }

        @Override
        public void warn(String message) {
            // Do nothing
        }

    }

    private static class WithLogger extends StartupLog {

        private final List<String> messages = new ArrayList<>();
        private final Logger logger;

        public WithLogger(Logger logger) {
            this.logger = Objects.requireNonNull(logger, "logger");
        }

        @Override
        public List<String> getMessages() {
            return Collections.unmodifiableList(messages);
        }

        @Override
        public void severe(String message) {
            messages.add(message);
            logger.severe(message);
        }

        @Override
        public void warn(String message) {
            messages.add(message);
            logger.warning(message);
        }
    }

    /**
     * Gets a logger that discards all messages.
     *
     * @return A discarding logger.
     */
    public static StartupLog discarding() {
        return new NullLogger();
    }

    /**
     * Gets a logger that stores all messages, but also forwards them to the
     * given logger.
     *
     * @param logger
     *            The logger to forward to.
     * @return The logger.
     */
    public static StartupLog wrapping(Logger logger) {
        return new WithLogger(logger);
    }

    /**
     * Gets all messages displayed up until now.
     *
     * @return All messages.
     */
    public abstract List<String> getMessages();

    public abstract void severe(String message);

    public abstract void warn(String message);
}
