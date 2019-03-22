package nl.rutgerkok.topographica.webserver;

import nl.rutgerkok.topographica.marker.MarkerCollection;

/**
 * A world, as seen by the web server.
 *
 */
public interface WebWorld {

    /**
     * Gets the display name of this world.
     *
     * @return The display name.
     */
    String getDisplayName();

    /**
     * Gets the folder name of this world.
     *
     * @return The folder name.
     */
    String getFolderName();

    /**
     * Gets the markers of this world. The returned list must be thread-safe.
     *
     * @return The markers.
     */
    MarkerCollection getMarkers();

    /**
     * Gets the block origin of the world, for example [0, 64, 0].
     *
     * @return The block origin, [x, y, z].
     */
    int[] getOrigin();
}
