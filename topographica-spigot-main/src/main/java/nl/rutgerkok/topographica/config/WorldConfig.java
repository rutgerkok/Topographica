package nl.rutgerkok.topographica.config;

import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_BLOCKS;
import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_BLOCKS_BITS;

import nl.rutgerkok.topographica.util.StartupLog;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public final class WorldConfig {

    static final String DEFAULT_WORLD = "default";

    private final int centerX;
    private final int centerZ;
    private final long radius;
    private final long radiusSquared;
    private final ColorConfig colorConfig;
    private final String displayName;
    private final int maxChunkLoadTimeNSPT;

    WorldConfig(World copyDefaults, String worldName, ConfigurationSection config, StartupLog log) {
        displayName = config.getString("display-name", worldName);
        Location spawn = copyDefaults.getSpawnLocation();
        centerX = config.getInt("center-x", spawn.getBlockX());
        centerZ = config.getInt("center-z", spawn.getBlockZ());
        maxChunkLoadTimeNSPT = config.getInt("max-blocking-time-nanoseconds-per-tick");
        int radius = config.getInt("radius");
        if (radius < REGION_SIZE_BLOCKS && radius != 0) {
            log.warn("The radius " + radius + " in world '" + worldName + "' was too small. Changed it to "
                    + REGION_SIZE_BLOCKS);
            radius = REGION_SIZE_BLOCKS;
        }
        this.radius = radius;
        radiusSquared = radius * radius;
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
     * Returns whether this world must be rendered at all.
     *
     * @return True if the world must be rendered, false otherwise.
     */
    public boolean isEnabled() {
        return this.radius > 0;
    }

    /**
     * Checks if the specified region should be rendered according to the
     * settings of this world.
     *
     * @param regionX
     *            X position of the region.
     * @param regionZ
     *            Z position of the region.
     * @return True if it should be rendered, false otherwise.
     */
    public boolean shouldRender(int regionX, int regionZ) {
        if (radius == Integer.MAX_VALUE) {
            return true;
        }
        if (radius == 0) {
            return false;
        }

        long blockX = regionX << REGION_SIZE_BLOCKS_BITS | (REGION_SIZE_BLOCKS / 2);
        long blockZ = regionZ << REGION_SIZE_BLOCKS_BITS | (REGION_SIZE_BLOCKS / 2);
        long distanceSquared = (blockX - centerX) * (blockX - centerX) + (blockZ - centerZ) * (blockZ - centerZ);
        return distanceSquared <= radiusSquared;
    }

    void write(ConfigurationSection section) {
        if (!displayName.equals(DEFAULT_WORLD)) {
            section.set("display-name", displayName);
        }
        section.set("center-x", centerX);
        section.set("center-z", centerZ);
        section.set("radius", radius);
        section.set("max-blocking-time-nanoseconds-per-tick", maxChunkLoadTimeNSPT);
        colorConfig.write(section.createSection("colors"));
    }
}
