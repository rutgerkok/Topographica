package nl.rutgerkok.topographica.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import nl.rutgerkok.topographica.marker.MapLocation;

/**
 * A bridge between Spigot-classes and {@link MapLocation}.
 *
 */
public final class MapLocations {

    /**
     * Creates a map point at the given block. The world of the block is
     * ignored.
     *
     * @param block
     *            The location.
     * @return The map point.
     */
    public static MapLocation ofBlock(Block block) {
        return MapLocation.of(block.getX(), block.getZ());
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
    public static MapLocation ofLocation(Location location) {
        return MapLocation.of(location.getBlockX(), location.getBlockZ());
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
    public static MapLocation ofVector(Vector vector) {
        return MapLocation.of(vector.getBlockX(), vector.getBlockZ());
    }

    private MapLocations() {
    // No instances!
    }
}
