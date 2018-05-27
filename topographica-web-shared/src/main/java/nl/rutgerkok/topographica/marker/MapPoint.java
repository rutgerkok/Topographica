package nl.rutgerkok.topographica.marker;

import org.json.simple.JSONAware;

/**
 * A point on the map. Does not store the world. Immutable value.
 */
public final class MapPoint implements JSONAware {

    /**
     * Creates a map point using the given coords.
     * 
     * <p>
     * Note: this method is not very convenient to use. In the Spigot-specific
     * packages, there are some methods that bridge Spigot objects (like Block
     * and Location) to a {@link MapPoint}. See the MapPoints class there.
     *
     * @param blockX
     *            Block x.
     * @param blockZ
     *            Block z.
     * @return The map point.
     */
    public static MapPoint of(int blockX, int blockZ) {
        return new MapPoint(blockX, blockZ);
    }

    private final int x;
    private final int z;

    private MapPoint(int x, int z) {
        this.x = x;
        this.z = z;
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
        MapPoint other = (MapPoint) obj;
        if (x != other.x) {
            return false;
        }
        if (z != other.z) {
            return false;
        }
        return true;
    }

    /**
     * Gets the block x.
     *
     * @return The block x.
     */
    public int getX() {
        return x;
    }

    /**
     * Gets the block z.
     *
     * @return The block z.
     */
    public int getZ() {
        return z;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + z;
        return result;
    }

    @Override
    public String toJSONString() {
        // Yeah, that's how the map represents coords
        return "[" + -z + ", " + x + "]";
    }

    @Override
    public String toString() {
        return "MapPoint [x=" + x + ", z=" + z + "]";
    }

}
