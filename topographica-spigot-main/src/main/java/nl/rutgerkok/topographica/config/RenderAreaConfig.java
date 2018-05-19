package nl.rutgerkok.topographica.config;

import static nl.rutgerkok.topographica.util.SizeConstants.CHUNK_SIZE_BLOCKS;
import static nl.rutgerkok.topographica.util.SizeConstants.CHUNK_SIZE_BLOCKS_BITS;
import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_BLOCKS;
import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_BLOCKS_BITS;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.BlockVector;

import nl.rutgerkok.topographica.util.Region;

/**
 * Class that determines exactly what areas of the world should be rendered.
 *
 */
public final class RenderAreaConfig {

    private final int centerX;
    private final int centerZ;
    private final long radius;
    private final long radiusSquared;

    public RenderAreaConfig(int centerX, int centerZ, long radius) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.radius = radius;
        this.radiusSquared = radius * radius;

        if (radius < 0) {
            throw new IllegalArgumentException("Negative radius: " + radius);
        }
    }

    /**
     * Gets the origin or center of the world.
     *
     * @return The origin.
     */
    public BlockVector getOrigin() {
        return new BlockVector(centerX, 65, centerZ);
    }

    /**
     * Checks if there is any area that should be rendered.
     *
     * @return Any area.
     */
    public boolean shouldRenderAnything() {
        return radius > 0;
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

    /**
     * Checks if the specified region should be rendered according to the
     * settings of this world.
     *
     * @param region
     *            The region.
     * @return True if it should be rendered, false otherwise.
     */
    public boolean shouldRenderRegion(Region region) {
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

    void write(ConfigurationSection section) {
        section.set("center-x", centerX);
        section.set("center-z", centerZ);
        section.set("radius", radius);
    }
}
