package nl.rutgerkok.topographica.webserver;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * A class for setting up the default logging settings.
 *
 */
final class ServerLogger {

    static {
        configure();
    }

    private static void configure() {
        Logger logger = setup(ServerLogger.class);
        if (System.getProperty("java.util.logging.config.file") != null) {
            logger.info("Found logging properties file - not making any customizations to logging.");
            return;
        }
        Logger parent = logger.getParent();
        if (parent == null) {
            return;
        }

        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %4$s] %5$s%6$s%n");

        // Replace the parent handlers
        for (Handler handler : parent.getHandlers()) {
            parent.removeHandler(handler);
        }
        parent.addHandler(new ConsoleHandler() {
            {
                setOutputStream(System.out);
            }
        });
        try {
            FileHandler handler = new FileHandler("web-server-%g.log", 10000, 3);
            handler.setFormatter(new SimpleFormatter());
            parent.addHandler(handler);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to setup file logging", e);
        }

    }

    public static Logger setup(Class<?> clazz) {
        return Logger.getLogger(clazz.getName().replace("nl.rutgerkok.", ""));
    }

}
