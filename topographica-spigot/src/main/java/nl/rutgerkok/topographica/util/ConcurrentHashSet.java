package nl.rutgerkok.topographica.util;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Easy access to an implementation of a concurrent set.
 *
 */
public final class ConcurrentHashSet {

    /**
     * Creates a new, empty, concurrent set.
     *
     * @return The set.
     */
    public static <T> Set<T> create() {
        return Collections.newSetFromMap(new ConcurrentHashMap<T, Boolean>());
    }

    private ConcurrentHashSet() {
        // No instances
    }
}
