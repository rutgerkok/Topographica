package nl.rutgerkok.topographica.config;

import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_BLOCKS;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import nl.rutgerkok.topographica.util.StartupLog;

public final class WorldConfig {

    static final String DEFAULT_WORLD = "default";

    private final RenderAreaConfig renderAreaConfig;
    private final ColorConfig colorConfig;
    private final String displayName;
    private final int maxChunkLoadTimeNSPT;

    WorldConfig(World copyDefaultsOrNull, String worldName, ConfigurationSection config, StartupLog log) {
        displayName = config.getString("display-name", worldName);
        Location spawn = copyDefaultsOrNull == null ? new Location(null, 0, 0, 0)
                : copyDefaultsOrNull.getSpawnLocation();
        maxChunkLoadTimeNSPT = config.getInt("max-blocking-time-nanoseconds-per-tick");
        int radius = config.getInt("radius");
        if (radius < 50 && radius != 0) {
            log.warn("The radius " + radius + " in world '" + worldName + "' was too small. Changed it to " + 50);
            radius = REGION_SIZE_BLOCKS;
        }
        renderAreaConfig = new RenderAreaConfig(
                config.getInt("center-x", spawn.getBlockX()),
                config.getInt("center-z", spawn.getBlockZ()),
                radius);
        colorConfig = new ColorConfig(config.getConfigurationSection("colors"), log);
    }

    /**
     * Gets the map colors of this world.
     *
     * @return The colors.
     */
    public ColorConfig getColors() {
        return colorConfig;
    }

    /**
     * Gets the name of this world, displayed to users.
     *
     * @return The name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * The amount of nanoseconds that may be spent reading chunks on the main
     * thread in each tick.
     *
     * @return The amount of nanoseconds.
     */
    public int getMaxChunkLoadTimeNSPT() {
        return maxChunkLoadTimeNSPT;
    }

    /**
     * Gets the area that should be visisble on the map.
     *
     * @return The area.
     */
    public RenderAreaConfig getRenderArea() {
        return renderAreaConfig;
    }

    /**
     * Returns whether this world must be rendered at all.
     *
     * @return True if the world must be rendered, false otherwise.
     */
    public boolean isEnabled() {
        return this.renderAreaConfig.shouldRenderAnything();
    }

    void write(ConfigurationSection section) {
        if (!displayName.equals(DEFAULT_WORLD)) {
            section.set("display-name", displayName);
        }
        this.renderAreaConfig.write(section);
        section.set("max-blocking-time-nanoseconds-per-tick", maxChunkLoadTimeNSPT);
        colorConfig.write(section.createSection("colors"));
    }
}
