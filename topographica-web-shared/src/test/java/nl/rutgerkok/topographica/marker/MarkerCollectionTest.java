package nl.rutgerkok.topographica.marker;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class MarkerCollectionTest {

    @Test
    public void basics() {
        MarkerCollection collection = new MarkerCollection();
        Marker testMarker = Marker.point(MapLocation.of(10, 8));
        UUID testUUID = new UUID(1, 2);

        collection.setMarker(testUUID, testMarker);

        // Check if list now contains one marker of the correct type
        List<Marker> allMarkers = stream(collection.spliterator(), false).collect(toList());
        assertEquals(1, allMarkers.size());
        assertEquals(testMarker, allMarkers.get(0));
    }

    @Test
    public void twoUuidList() {
        MarkerCollection collection = new MarkerCollection();
        collection.setMarker(new UUID(1, 2), Marker.point(MapLocation.of(10, 8)));
        collection.setMarker(new UUID(10, 20), Marker.point(MapLocation.of(12, 2)));

        List<Marker> allMarkers = stream(collection.spliterator(), false).collect(toList());
        assertEquals(2, allMarkers.size());
    }
}
