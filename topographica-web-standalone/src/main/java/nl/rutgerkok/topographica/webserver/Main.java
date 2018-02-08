package nl.rutgerkok.topographica.webserver;

import java.net.BindException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public final class Main {

    public static void main(String... args) {
        int port = 8088;
        if (args.length > 1) {
            System.err.println("Args error. Correct syntax: java -jar Topographica-Webserver.jar <port>");
            System.exit(100);
        }
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("help")) {
                System.out.println("Runs a web server in the current working directory.");
                System.out.println("java -jar Topographica-Webserver.jar <port>");
            }
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Args error. Invalid port number: " + args[0]);
                System.exit(101);
            }
        }

        Path imagesFolder = Paths.get(System.getProperty("webPaths.images", WebPaths.IMAGES));
        if (!Files.exists(imagesFolder) || !Files.isDirectory(imagesFolder)) {
            System.err.println("No " + WebPaths.IMAGES + " folder found at " + imagesFolder.toAbsolutePath());
            System.exit(102);
        }
        new Main(imagesFolder, port);
    }

    private final WebServer webServer;

    public Main(Path imagesFolder, int port) {
        this.webServer = startWebServer(imagesFolder, port);

        new Console(webServer).start();
    }

    private WebServer startWebServer(Path imagesFolder, int port) {
        Logger logger = ServerLogger.setup(WebServer.class);
        try {
            return new WebServer(new JarOrResourceFileRetriever(), new LastKnownServerInfo(imagesFolder, port), logger);
        } catch (BindException e) {
            logger.severe("Failed to bind to port " + port);
            System.exit(1);
            return null;
        }
    }
}
