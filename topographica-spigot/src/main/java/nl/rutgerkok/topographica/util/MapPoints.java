package nl.rutgerkok.topographica.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import nl.rutgerkok.topographica.marker.MapPoint;

/**
 * A bridge between Spigot-classes and {@link MapPoint}.
 *
 */
public final class MapPoints {

    /**
     * Creates a map point at the given block. The world of the block is
     * ignored.
     *
     * @param block
     *            The location.
     * @return The map point.
     */
    public static MapPoint ofBlock(Block block) {
        return MapPoint.of(block.getX(), block.getZ());
    }
    /**
     * Creates a map point at the given location. The world of the location is
     * ignored. If you modify the coords of the location, this map point will
     * <em>not</em> be updated accordingly.
     *
     * @param location
     *            The location.
     * @return The map point.
     */
    public static MapPoint ofLocation(Location location) {
        return MapPoint.of(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Creates a map point at the given vector of block coords. If you modify
     * the coords of the vector, this map point will <em>not</em> be updated
     * accordingly.
     *
     * @param vector
     *            The location.
     * @return The map point.
     */
    public static MapPoint ofVector(Vector vector) {
        return MapPoint.of(vector.getBlockX(), vector.getBlockZ());
    }

    private MapPoints() {
    // No instances!
    }
}
