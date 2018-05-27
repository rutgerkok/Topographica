package nl.rutgerkok.topographica.marker;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

/**
 * A marker, displayed on the map.
 *
 */
public class Marker implements JSONStreamAware {

    /**
     * A polygon marker.
     *
     */
    public static class Polygon extends Marker {

        Polygon(String method, JSONAware firstParam, JSONObject secondParam, HtmlString tooltipOrNull) {
            super(method, firstParam, secondParam, tooltipOrNull);
        }

        @Override
        public Polygon tooltip(HtmlString tooltip) {
            Objects.requireNonNull(tooltip, "tooltip");
            return new Polygon(method, firstParam, secondParam, tooltip);
        }

        /**
         * Creates a new marker with the given style. Make sure that no other
         * thread is modifying the {@link PolygonStyle} object while this method
         * is called.
         *
         * @param style
         *            The style.
         * @return The new marker.
         */
        @SuppressWarnings("unchecked")
        public Polygon withStyle(PolygonStyle style) {
            Objects.requireNonNull(style, "style");
            JSONObject secondParam = new JSONObject();
            secondParam.putAll(this.secondParam);
            secondParam.putAll(style.options);
            return new Polygon(method, firstParam, secondParam, tooltipOrNull);
        }
    }

    /**
     * A circle.
     *
     * @param center
     *            Center of the circle.
     * @param radius
     *            Radius of the circle, in blocks.
     * @return The circle.
     */
    @SuppressWarnings("unchecked")
    public static Polygon circle(MapPoint center, int radius) {
        JSONObject options = new JSONObject();
        options.put("radius", radius);
        return new Polygon("circle", center, options, null);
    }

    /**
     * A typical map marker.
     *
     * @param point
     *            The point.
     * @return The point marker.
     */
    public static Marker point(MapPoint point) {
        return new Marker("marker", point, new JSONObject(), null);
    }

    /**
     * A polygon: can be any (closed) shape. Do not include the first point as
     * the final point.
     *
     * @param points
     *            The points.
     * @return The polygon.
     */
    @SuppressWarnings("unchecked")
    public static Polygon polygon(List<MapPoint> points) {
        JSONArray array = new JSONArray();
        array.addAll(points);
        return new Polygon("polygon", array, new JSONObject(), null);
    }

    /**
     * A polygon: can be any (closed) shape.
     *
     * @param points
     *            The points.
     * @return The polygon.
     */
    public static Polygon polygon(MapPoint... points) {
        return polygon(Arrays.asList(points));
    }

    /**
     * A rectangle.
     *
     * @param lowestXZ
     *            Starting point.
     * @param highestXZ
     *            Final point.
     * @return The rectangle.
     */
    public static Marker rectangle(MapPoint lowestXZ, MapPoint highestXZ) {
        MapPoint lowestXHighestZ = MapPoint.of(lowestXZ.getX(), highestXZ.getZ());
        MapPoint highestXLowestZ = MapPoint.of(highestXZ.getX(), lowestXZ.getZ());
        return polygon(lowestXZ, highestXLowestZ, highestXZ, lowestXHighestZ);
    }

    protected final String method;
    /**
     * May not be mutated to keep this class thread-safe and safe for storage.
     */
    protected final JSONAware firstParam;
    /**
     * May not be mutated to keep this class thread-safe and safe for storage.
     */
    protected final JSONObject secondParam;
    protected final HtmlString tooltipOrNull;

    Marker(String method, JSONAware firstParam, JSONObject secondParam, HtmlString tooltipOrNull) {
        this.method = Objects.requireNonNull(method, "method");
        this.firstParam = Objects.requireNonNull(firstParam, "firstParam");
        this.secondParam = Objects.requireNonNull(secondParam, "secondParam");
        this.tooltipOrNull = tooltipOrNull;
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
        Marker other = (Marker) obj;
        if (!firstParam.equals(other.firstParam)) {
            return false;
        }
        if (!method.equals(other.method)) {
            return false;
        }
        if (!secondParam.equals(other.secondParam)) {
            return false;
        }
        if (!tooltipOrNull.equals(other.tooltipOrNull)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + firstParam.hashCode();
        result = prime * result + method.hashCode();
        result = prime * result + secondParam.hashCode();
        result = prime * result + tooltipOrNull.hashCode();
        return result;
    }

    /**
     * Creates a new marker with the given tooltip. Can be called from any
     * thread.
     *
     * @param tooltip
     *            The tooltip, may not be null.
     * @return The new marker.
     */
    public Marker tooltip(HtmlString tooltip) {
        Objects.requireNonNull(tooltip, "tooltip");
        return new Marker(this.method, this.firstParam, this.secondParam, tooltip);
    }

    /**
     * {@inheritDoc} This method is thread-safe.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONObject object = new JSONObject();
        object.put("method", method);
        object.put("firstParam", firstParam);
        object.put("secondParam", secondParam);
        if (this.tooltipOrNull != null) {
            object.put("tooltip", tooltipOrNull);
        }
        object.writeJSONString(out);
    }

}
