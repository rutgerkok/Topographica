package nl.rutgerkok.topographica.config;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.common.collect.ImmutableMap;

import nl.rutgerkok.topographica.util.Logg;

public final class Config {

    private final Map<String, WorldConfig> configsByWorld;
    private final WebConfig webConfig;

    public Config(Server server, FileConfiguration config, Logg log) {
        this.configsByWorld = getWorldConfigs(server, config, log);
        this.webConfig = new WebConfig(config.getConfigurationSection("webserver"), log);
    }

    public WorldConfig getConfig(World world) {
        WorldConfig result = configsByWorld.get(world.getName().toLowerCase(Locale.ROOT));
        if (result == null) {
            return configsByWorld.get("default");
        }
        return result;
    }

    /**
     * Gets the configuration for the web server.
     * @return The configuration.
     */
    public WebConfig getWebConfig() {
        return webConfig;
    }

    private Map<String, WorldConfig> getWorldConfigs(Server server, FileConfiguration config, Logg log) {
        Map<String, WorldConfig> configsByWorld = new HashMap<>();

        // Put some default values
        World defaultWorld = server.getWorlds().get(0);
        configsByWorld.put("default", new WorldConfig(defaultWorld, new YamlConfiguration(), log));

        // Read actual values
        ConfigurationSection worldSection = config.getConfigurationSection("worlds");

        for (String key : worldSection.getKeys(false)) {
            ConfigurationSection section = worldSection.getConfigurationSection(key);
            if (section == null) {
                log.warn("The world settings of the world " + key
                        + " are not a valid YAML section. Check your config for syntax errors.");
                continue;
            }
            World world = server.getWorld(key);
            if (world == null) {
                world = defaultWorld;
            }
            configsByWorld.put(key.toLowerCase(Locale.ROOT), new WorldConfig(world, section, log));
        }

        return ImmutableMap.copyOf(configsByWorld);
    }

    private void wipeConfig(FileConfiguration to) {
        for (String key : to.getKeys(false)) {
            to.set(key, null);
        }
    }

    /**
     * Writes all settings to the given configuration file.
     * 
     * @param to
     *            Configuration file.
     */
    public void write(FileConfiguration to) {
        wipeConfig(to);

        webConfig.write(to.createSection("webserver"));

        for (Entry<String, WorldConfig> worldConfig : configsByWorld.entrySet()) {
            ConfigurationSection section = to.createSection("worlds." + worldConfig.getKey());
            worldConfig.getValue().write(section);
        }
    }

}
