package nl.rutgerkok.topographica.marker;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;

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
     * A line in between points.
     *
     */
    public static class Line extends Marker {

        Line(String method, JSONAware firstParam, ImmutableMap<String, Object> secondParam, HtmlString tooltipOrNull) {
            super(method, firstParam, secondParam, tooltipOrNull);
        }

        @Override
        public Line tooltip(HtmlString tooltip) {
            // Overridden to keep the Polygon type
            Objects.requireNonNull(tooltip, "tooltip");
            return new Line(method, firstParam, secondParam, tooltip);
        }

        /**
         * Creates a new line with the given style. Make sure that no other
         * thread is modifying the {@link PolygonStyle} object while this method
         * is called.
         *
         * @param style
         *            The style.
         * @return The new line.
         */
        public Line withStyle(LineStyle style) {
            Objects.requireNonNull(style, "style");
            ImmutableMap.Builder<String, Object> secondParam = ImmutableMap.builder();
            secondParam.putAll(this.secondParam);
            secondParam.putAll(style.options);
            return new Line(method, firstParam, secondParam.build(), tooltipOrNull);
        }

    }

    /**
     * A polygon marker.
     *
     */
    public static class Polygon extends Marker {

        Polygon(String method, JSONAware firstParam, ImmutableMap<String, Object> secondParam,
                HtmlString tooltipOrNull) {
            super(method, firstParam, secondParam, tooltipOrNull);
        }

        @Override
        public Polygon tooltip(HtmlString tooltip) {
            // Overridden to keep the Polygon type
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
        public Polygon withStyle(PolygonStyle style) {
            Objects.requireNonNull(style, "style");
            ImmutableMap.Builder<String, Object> secondParam = ImmutableMap.builder();
            secondParam.putAll(this.secondParam);
            secondParam.putAll(style.options);
            return new Polygon(method, firstParam, secondParam.build(), tooltipOrNull);
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
    public static Polygon circle(MapLocation center, int radius) {
        ImmutableMap<String, Object> options = ImmutableMap.of("radius", radius);
        return new Polygon("circle", center, options, null);
    }

    /**
     * A line between the given points.
     *
     * @param points
     *            The points. Provide at least two points.
     * @return The line.
     */
    @SuppressWarnings("unchecked")
    public static Line line(List<MapLocation> points) {
        JSONArray array = new JSONArray();
        array.addAll(points);
        if (array.size() < 2) {
            throw new IllegalArgumentException("Less than two points were given: " + array);
        }
        return new Line("polyline", array, ImmutableMap.of(), null);
    }

    /**
     * A line between the given points.
     *
     * @param points
     *            The points. Provide at least two points.
     * @return The line.
     */
    public static Line line(MapLocation... points) {
        return line(Arrays.asList(points));
    }

    /**
     * A typical map marker.
     *
     * @param point
     *            The point.
     * @return The point marker.
     */
    public static Marker point(MapLocation point) {
        return new Marker("marker", point, ImmutableMap.of(), null);
    }

    /**
     * A polygon: can be any (closed) shape. You give a list of points
     * describing the shape. The shape will automatically be closed. Therefore,
     * there is <strong>no</strong> need to manually close the shape by making
     * the first and last point equal.
     *
     * @param points
     *            The points.
     * @return The polygon.
     */
    @SuppressWarnings("unchecked")
    public static Polygon polygon(List<MapLocation> points) {
        JSONArray array = new JSONArray();
        array.addAll(points);
        return new Polygon("polygon", array, ImmutableMap.of(), null);
    }

    /**
     * A polygon: can be any (closed) shape.
     *
     * @param points
     *            The points.
     * @return The polygon.
     */
    public static Polygon polygon(MapLocation... points) {
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
    public static Marker rectangle(MapLocation lowestXZ, MapLocation highestXZ) {
        MapLocation lowestXHighestZ = MapLocation.of(lowestXZ.getX(), highestXZ.getZ());
        MapLocation highestXLowestZ = MapLocation.of(highestXZ.getX(), lowestXZ.getZ());
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
    protected final ImmutableMap<String, Object> secondParam;
    protected final HtmlString tooltipOrNull;

    Marker(String method, JSONAware firstParam, ImmutableMap<String, Object> secondParam, HtmlString tooltipOrNull) {
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
            object.put("tooltip", tooltipOrNull.toString());
        }
        object.writeJSONString(out);
    }

}
