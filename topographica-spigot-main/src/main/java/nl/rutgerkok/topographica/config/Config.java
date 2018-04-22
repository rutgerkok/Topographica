package nl.rutgerkok.topographica.config;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import nl.rutgerkok.topographica.util.StartupLog;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class Config {

    private static Function<? super World, ? extends String> getNameLowercase = new Function<World, String>() {

        @Override
        public String apply(World world) {
            return world.getName().toLowerCase(Locale.ROOT);
        }
    };

    private final Map<String, WorldConfig> configsByWorld;
    private final WebConfig webConfig;

    public Config(Server server, FileConfiguration config, Path dataFolder, StartupLog log) {
        this.configsByWorld = getWorldConfigs(server, config, log);
        this.webConfig = new WebConfig(config.getConfigurationSection("web-server"), dataFolder, log);
    }

    public WorldConfig getConfig(World world) {
        return getWorldConfig(world.getName());
    }

    /**
     * Gets the configuration for the web server.
     *
     * @return The configuration.
     */
    public WebConfig getWebConfig() {
        return webConfig;
    }

    /**
     * Gets the configs for the given world, or the default if there are no
     * settings for this world.
     *
     * @param worldName
     *            Name of the world. ({@link World#getName()})
     * @return The world config.
     */
    public WorldConfig getWorldConfig(String worldName) {
        WorldConfig result = configsByWorld.get(worldName.toLowerCase(Locale.ROOT));
        if (result == null) {
            return configsByWorld.get("default");
        }
        return result;
    }

    private Map<String, WorldConfig> getWorldConfigs(Server server, FileConfiguration config, StartupLog log) {
        Map<String, WorldConfig> configsByWorld = new HashMap<>();

        // Put some default values
        World defaultWorld = server.getWorlds().get(0);
        ConfigurationSection defaultWorldSection = config.getConfigurationSection("worlds.default");
        WorldConfig defaultWorldConfig = new WorldConfig(defaultWorld, "default", defaultWorldSection, log);
        configsByWorld.put("default", defaultWorldConfig);
        // Let other worlds use updated defaults:
        defaultWorldConfig.write(defaultWorldSection);

        // Determine worlds to read
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        Set<String> worldNames = worldsSection.getKeys(false);
        worldNames.addAll(Lists.transform(server.getWorlds(), getNameLowercase));
        worldNames.remove("default");

        // Read worlds
        for (String worldName : worldNames) {
            ConfigurationSection worldSection = worldsSection.getConfigurationSection(worldName);
            if (worldSection == null) {
                if (worldsSection.get(worldName) != null) {
                    // There is something that is not a section
                    log.warn("The world settings of the world " + worldName
                            + " are not a valid YAML section. Check your config for syntax errors.");
                }
                worldSection = worldsSection.createSection(worldName);
            }

            World world = MoreObjects.firstNonNull(server.getWorld(worldName), defaultWorld);
            setAsDefaults(defaultWorldSection, worldSection);

            configsByWorld.put(worldName.toLowerCase(Locale.ROOT),
                    new WorldConfig(world, worldName, worldSection, log));
        }

        return ImmutableMap.copyOf(configsByWorld);
    }

    private void pruneDefaults(ConfigurationSection defaults, ConfigurationSection actual) {
        for (Entry<String, Object> entry : actual.getValues(true).entrySet()) {
            if (entry.getValue().equals(defaults.get(entry.getKey()))) {
                actual.set(entry.getKey(), null);
            }
        }

    }

    private void setAsDefaults(ConfigurationSection defaults, ConfigurationSection addTo) {
        for (Entry<String, Object> value : defaults.getValues(true).entrySet()) {
            addTo.addDefault(value.getKey(), value.getValue());
        }
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

        webConfig.write(to.createSection("web-server"));

        ConfigurationSection defaultWorldSection = to.createSection("worlds.default");
        configsByWorld.get("default").write(defaultWorldSection);
        for (Entry<String, WorldConfig> worldConfig : configsByWorld.entrySet()) {
            if (worldConfig.getKey().equals("default")) {
                continue;
            }
            ConfigurationSection section = to.createSection("worlds." + worldConfig.getKey());
            worldConfig.getValue().write(section);
            pruneDefaults(defaultWorldSection, section);
        }
    }

}
