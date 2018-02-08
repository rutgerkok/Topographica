package nl.rutgerkok.topographica.webserver;

/**
 * Information of a player, needed for the web server.
 *
 */
public interface WebPlayer {

    /**
     * Name of the player.
     *
     * @return The name.
     */
    public abstract String getDisplayName();

    /**
     * Gets the x/z position of the player.
     *
     * @return The x/z position.
     * @see IntPair
     */
    public abstract long getPosition();
}
