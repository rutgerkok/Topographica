package nl.rutgerkok.topographica.util;

import nl.rutgerkok.topographica.marker.MapLocation;

/**
 * Represents some abstract x/z pair. Unlike {@link MapLocation}, this class
 * does not necessarily use block coordinates, but may use some other (scaled)
 * coordinate system.
 */
public final class Coordinate {
    public final int x;
    public final int z;

    public Coordinate(int x, int z) {
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
        Coordinate other = (Coordinate) obj;
        if (x != other.x) {
            return false;
        }
        if (z != other.z) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + z;
        return result;
    }
}
