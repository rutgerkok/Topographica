package nl.rutgerkok.topographica.config;

import static nl.rutgerkok.topographica.util.SizeConstants.CHUNK_SIZE_BLOCKS;
import static nl.rutgerkok.topographica.util.SizeConstants.CHUNK_SIZE_BLOCKS_BITS;
import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_BLOCKS;
import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_BLOCKS_BITS;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import nl.rutgerkok.topographica.util.Region;
import nl.rutgerkok.topographica.util.StartupLog;

public final class WorldConfig {

    static final String DEFAULT_WORLD = "default";

    private final int centerX;
    private final int centerZ;
    private final long radius;
    private final long radiusSquared;
    private final ColorConfig colorConfig;
    private final String displayName;
    private final int maxChunkLoadTimeNSPT;

    WorldConfig(World copyDefaultsOrNull, String worldName, ConfigurationSection config, StartupLog log) {
        displayName = config.getString("display-name", worldName);
        Location spawn = copyDefaultsOrNull == null ? new Location(null, 0, 0, 0)
                : copyDefaultsOrNull.getSpawnLocation();
        centerX = config.getInt("center-x", spawn.getBlockX());
        centerZ = config.getInt("center-z", spawn.getBlockZ());
        maxChunkLoadTimeNSPT = config.getInt("max-blocking-time-nanoseconds-per-tick");
        int radius = config.getInt("radius");
        if (radius < 50 && radius != 0) {
            log.warn("The radius " + radius + " in world '" + worldName + "' was too small. Changed it to " + 50);
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
     * @param region
     *            The region.
     * @return True if it should be rendered, false otherwise.
     */
    public boolean shouldRender(Region region) {
        if (radius == Integer.MAX_VALUE) {
            return true;
        }
        if (radius == 0) {
            return false;
        }

        long blockX = region.getRegionX() << REGION_SIZE_BLOCKS_BITS;
        long blockZ = region.getRegionZ() << REGION_SIZE_BLOCKS_BITS;
        if (blockX < centerX) {
            blockX += Math.min(REGION_SIZE_BLOCKS - 1, centerX - blockX);
        }
        if (blockZ < centerZ) {
            blockZ += Math.min(REGION_SIZE_BLOCKS - 1, centerZ - blockZ);
        }

        long distanceSquared = (blockX - centerX) * (blockX - centerX) + (blockZ - centerZ) * (blockZ - centerZ);
        return distanceSquared <= radiusSquared;
    }

    public boolean shouldRenderChunk(int chunkX, int chunkZ) {
        if (radius == Integer.MAX_VALUE) {
            return true;
        }
        if (radius == 0) {
            return false;
        }

        long blockX = chunkX << CHUNK_SIZE_BLOCKS_BITS;
        long blockZ = chunkZ << CHUNK_SIZE_BLOCKS_BITS;
        if (blockX < centerX) {
            blockX += Math.min(CHUNK_SIZE_BLOCKS - 1, centerX - blockX);
        }
        if (blockZ < centerZ) {
            blockZ += Math.min(CHUNK_SIZE_BLOCKS - 1, centerZ - blockZ);
        }

        long distanceSquared = (blockX - centerX) * (blockX - centerX) + (blockZ - centerZ) * (blockZ - centerZ);
        return distanceSquared <= radiusSquared;

    }

    public boolean shouldRenderColumn(int blockX, int blockZ) {
        if (radius == Integer.MAX_VALUE) {
            return true;
        }
        if (radius == 0) {
            return false;
        }

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
