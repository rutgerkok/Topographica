package nl.rutgerkok.topographica.webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

final class Console extends Thread {

    private final WebServer webServer;
    private final Logger logger = ServerLogger.setup(Console.class);

    Console(WebServer webServer) {
        this.webServer = Objects.requireNonNull(webServer, "webServer");
    }

    @Override
    public void run() {
        logger.info("Web server is running. Type stop + ENTER to quit.");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            while (true) {
                String line = reader.readLine();
                if (line.equals("stop")) {
                    logger.info("Stopping web server...");
                    this.webServer.disable();
                    logger.info("Web server stopped. Goodbye!");
                    break;
                }
                logger.info("Unknown command: " + line);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading console. Stopping web server", e);
            webServer.disable();
            logger.info("Web server stopped.");
            System.exit(1);
        }
    }

}
