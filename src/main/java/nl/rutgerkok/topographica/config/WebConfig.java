package nl.rutgerkok.topographica.config;

import org.bukkit.configuration.ConfigurationSection;

import nl.rutgerkok.topographica.util.Logg;

public final class WebConfig {

    private final int port;

    WebConfig(ConfigurationSection config, Logg log) {
        int port = config.getInt("port");
        if (port <= 0) {
            log.warn("Ignoring invalid web server port: " + port);
            port = config.getDefaultSection().getInt("port");
        }
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    void write(ConfigurationSection config) {
        config.set("port", port);
    }
}
