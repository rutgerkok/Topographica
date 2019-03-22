package nl.rutgerkok.topographica.marker;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe class to hold map markers. Markers are categorized by an UUID,
 * which allows for easy removal.
 *
 */
public final class MarkerCollection implements Iterable<Marker> {

    private final Map<UUID, Marker[]> markers = new ConcurrentHashMap<>();

    /**
     * Deletes all markers with the given UUID.
     *
     * @param uuid
     *            The UUID of the markers.
     */
    public void deleteMarkers(UUID uuid) {
        this.markers.remove(uuid);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Note that an iterator instance can only be used by one thread at a time.
     * However, different threads can use different iterator objects to iterate
     * over this collection concurrently.
     */
    @Override
    public Iterator<Marker> iterator() {
        Iterator<Marker[]> arrayIterator = markers.values().iterator();

        return new Iterator<Marker>() {

            Marker[] currentArray;
            int posInArray;

            @Override
            public boolean hasNext() {
                if (currentArray != null && posInArray < currentArray.length - 1) {
                    return true;
                }
                return arrayIterator.hasNext();
            }

            @Override
            public Marker next() {
                if (currentArray == null || posInArray >= currentArray.length) {
                    // Go to next array
                    posInArray = 0;
                    currentArray = arrayIterator.next();
                } else {
                    // Advance in current array
                    posInArray++;
                }
                return currentArray[posInArray];
            }
        };
    }

    /**
     * Sets a single marker on the map.
     *
     * @param uuid
     *            The UUID of the marker. Any existing marker with this UUID
     *            will be overwritten.
     * @param marker
     *            The marker.
     */
    public void setMarker(UUID uuid, Marker marker) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(marker, "marker");

        Marker[] markerArray = new Marker[1];
        markerArray[0] = marker;
        this.markers.put(uuid, markerArray);
    }

    /**
     * Sets multiple markers to be assigned to the same UUID. This is useful if
     * you need multiple markers to represent a single object.
     *
     * @param uuid
     *            The UUID. Any existing markers with that UUID will be removed.
     * @param markers
     *            The markers with that UUID.
     */
    public void setMarkers(UUID uuid, Collection<Marker> markers) {
        Objects.requireNonNull(uuid, "uuid");

        Marker[] markerArray = markers.toArray(new Marker[0]);
        this.markers.put(uuid, markerArray);
    }
}
