package nl.rutgerkok.topographica.config;

import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_BLOCKS;
import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_BLOCKS_BITS;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import nl.rutgerkok.topographica.util.StartupLog;

public final class WorldConfig {

    static final String DEFAULT_WORLD = "default";

    private final int centerX;
    private final int centerZ;
    private final long radius;
    private final long radiusSquared;
    private final ColorConfig colorConfig;
    private final String displayName;

    WorldConfig(World copyDefaults, String worldName, ConfigurationSection config, StartupLog log) {
        displayName = config.getString("display-name", worldName);
        Location spawn = copyDefaults.getSpawnLocation();
        centerX = config.getInt("center-x", spawn.getBlockX());
        centerZ = config.getInt("center-z", spawn.getBlockZ());
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
        colorConfig.write(section.createSection("colors"));
    }
}