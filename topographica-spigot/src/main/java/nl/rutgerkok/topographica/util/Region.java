package nl.rutgerkok.topographica.util;

import org.bukkit.block.Block;

/**
 * A Minecraft region. 512x512 blocks in size.
 *
 */
public final class Region {

    public static Region of(int regionX, int regionZ) {
        return new Region(regionX, regionZ);
    }

    /**
     * Gets the region that contains the given block.
     *
     * @param block
     *            The block.
     * @return The region.
     */
    public static Region ofBlock(Block block) {
        return of(block.getX() >> 9, block.getZ() >> 9);
    }

    /**
     * Gets the region that holds the given chunk.
     *
     * @param chunkX
     *            The chunk x.
     * @param chunkZ
     *            The chunk x.
     * @return The region.
     */
    public static Region ofChunk(int chunkX, int chunkZ) {
        return new Region(chunkX >> 5, chunkZ >> 5);
    }

    private final int regionX;
    private final int regionZ;

    private Region(int regionX, int regionZ) {
        this.regionX = regionX;
        this.regionZ = regionZ;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Region other = (Region) obj;
        if (regionX != other.regionX) {
            return false;
        }
        if (regionZ != other.regionZ) {
            return false;
        }
        return true;
    }

    public int getRegionX() {
        return regionX;
    }

    public int getRegionZ() {
        return regionZ;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + regionX;
        result = prime * result + regionZ;
        return result;
    }

    @Override
    public String toString() {
        return "Region [regionX=" + regionX + ", regionZ=" + regionZ + "]";
    }
}
