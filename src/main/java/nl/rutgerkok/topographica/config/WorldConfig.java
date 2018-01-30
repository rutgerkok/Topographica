package nl.rutgerkok.topographica.config;

import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_BLOCKS;
import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_BLOCKS_BITS;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import nl.rutgerkok.topographica.util.Logg;
public final class WorldConfig {

    private int centerX;
    private int centerZ;
    private long radius;
    private long radiusSquared;

    WorldConfig(World world, ConfigurationSection config, Logg log) {
        Location spawn = world.getSpawnLocation();
        centerX = config.getInt("centerX", spawn.getBlockX());
        centerZ = config.getInt("centerZ", spawn.getBlockZ());
        radius = config.getInt("renderRadius");
        if (radius < REGION_SIZE_BLOCKS && radius != 0) {
            log.warn("The radius " + radius + " in world " + world.getName() + " was too small. Changed it to "
                    + REGION_SIZE_BLOCKS);
            radius = REGION_SIZE_BLOCKS;
        }
        radiusSquared = radius * radius;
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
        section.set("centerX", centerX);
        section.set("centerZ", centerZ);
        section.set("radius", radius);
    }
}
