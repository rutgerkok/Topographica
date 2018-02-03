package nl.rutgerkok.topographica.webserver;

import java.nio.file.Path;

/**
 * Holds the settings for the web server.
 *
 */
public interface WebConfigInterface {

    /**
     * Gets the folder where the images of all worlds are stored.
     *
     * @return The folder.
     */
    Path getImagesFolder();

    /**
     * Gets the port the web server must run at.
     *
     * @return The port.
     */
    int getPort();

}
