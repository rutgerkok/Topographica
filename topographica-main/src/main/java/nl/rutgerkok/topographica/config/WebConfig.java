package nl.rutgerkok.topographica.config;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

import org.bukkit.configuration.ConfigurationSection;

import nl.rutgerkok.topographica.util.StartupLog;

public final class WebConfig {

    private final int port;
    private final String webFolder;
    private final Path pluginDataFolder;

    WebConfig(ConfigurationSection config, Path pluginDataFolder, StartupLog log) {
        this.pluginDataFolder = Objects.requireNonNull(pluginDataFolder);

        this.webFolder = config.getString("web-root");
        int port = config.getInt("port");
        if (port <= 0) {
            log.warn("Ignoring invalid web server port: " + port);
            port = config.getDefaultSection().getInt("port");
        }
        this.port = port;
    }

    /**
     * Gets the path where images are stored.
     *
     * @return The image path.
     */
    public Path getImagesFolder() {
        return getWebFolder().resolve("images");
    }

    /**
     * Gets the port on which the web server runs.
     *
     * @return The port.
     */
    public int getPort() {
        return port;
    }

    private Path getWebFolder() {
        File webFolder = new File(this.webFolder);
        if (webFolder.isAbsolute()) {
            return webFolder.toPath();
        }
        return pluginDataFolder.resolve(this.webFolder);
    }

    void write(ConfigurationSection config) {
        config.set("port", port);
        config.set("web-root", webFolder);
    }
}
