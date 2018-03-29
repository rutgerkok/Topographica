package nl.rutgerkok.topographica.webserver;

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
}