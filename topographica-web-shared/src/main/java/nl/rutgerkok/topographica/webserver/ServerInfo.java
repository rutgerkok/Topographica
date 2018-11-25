package nl.rutgerkok.topographica.webserver;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

/**
 * Reads out many different properties of the server environment, like the
 * online players and the available worlds.
 *
 * <p>
 * All methods can be called from any thread.
 */
public abstract class ServerInfo {

    /**
     * Gets the folder where the images of all worlds are stored.
     *
     * @return The folder.
     */
    public abstract Path getImagesFolder();

    /**
     * Gets a collection of all players visible in a world.
     *
     * @param world
     *            The world.
     * @return The players.
     */
    public abstract Collection<? extends WebPlayer> getPlayers(WebWorld world);

    /**
     * Gets the port the web server must run at.
     *
     * @return The port.
     */
    public abstract int getPort();

    /**
     * Gets the world with the given folder name. Case sensitive.
     *
     * @param worldName
     *            World folder name.
     * @return The world, or empty if not found.
     */
    public Optional<WebWorld> getWorld(String worldName) {
        for (WebWorld world : getWorlds()) {
            if (world.getFolderName().equals(worldName)) {
                return Optional.of(world);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets a collection of all worlds visible on the map.
     *
     * @return The worlds.
     */
    public abstract Collection<? extends WebWorld> getWorlds();
}
