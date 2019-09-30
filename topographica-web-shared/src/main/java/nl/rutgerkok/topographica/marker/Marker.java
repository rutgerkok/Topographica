package nl.rutgerkok.topographica.marker;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A marker, displayed on the map.
 *
 */
public class Marker implements JsonAware {

    /**
     * A line in between points.
     *
     */
    public static class Line extends Marker {

        Line(String method, JsonElement element, JsonObject jsonObject, HtmlString tooltipOrNull) {
            super(method, element, jsonObject, tooltipOrNull);
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

            JsonObject secondParam = new JsonObject();
            for (Entry<String, JsonElement> entry : this.secondParam.entrySet()) {
                secondParam.add(entry.getKey(), entry.getValue());
            }
            for (Entry<String, JsonElement> entry : style.options.entrySet()) {
                secondParam.add(entry.getKey(), entry.getValue());
            }

            return new Line(method, firstParam, secondParam, tooltipOrNull);
        }

    }

    /**
     * A polygon marker.
     *
     */
    public static class Polygon extends Marker {

        Polygon(String method, JsonElement firstParam, JsonObject secondParam,
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

            JsonObject secondParam = new JsonObject();
            for (Entry<String, JsonElement> entry : this.secondParam.entrySet()) {
                secondParam.add(entry.getKey(), entry.getValue());
            }
            for (Entry<String, JsonElement> entry : style.options.entrySet()) {
                secondParam.add(entry.getKey(), entry.getValue());
            }

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
    public static Polygon circle(MapLocation center, int radius) {
        JsonObject options = new JsonObject();
        options.addProperty("radius", radius);
        return new Polygon("circle", center.toJsonElement(), options, null);
    }

    /**
     * A line between the given points.
     *
     * @param points
     *            The points. Provide at least two points.
     * @return The line.
     */
    public static Line line(List<MapLocation> points) {
        JsonArray array = new JsonArray();
        for (MapLocation point : points) {
            array.add(point.toJsonElement());
        }
        if (array.size() < 2) {
            throw new IllegalArgumentException("Less than two points were given: " + array);
        }
        return new Line("polyline", array, new JsonObject(), null);
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
        return new Marker("marker", point.toJsonElement(), new JsonObject(), null);
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
    public static Polygon polygon(List<MapLocation> points) {
        JsonArray array = new JsonArray();
        for (MapLocation point : points) {
            array.add(point.toJsonElement());
        }
        return new Polygon("polygon", array, new JsonObject(), null);
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
    public static Polygon rectangle(MapLocation lowestXZ, MapLocation highestXZ) {
        MapLocation lowestXHighestZ = MapLocation.of(lowestXZ.getX(), highestXZ.getZ());
        MapLocation highestXLowestZ = MapLocation.of(highestXZ.getX(), lowestXZ.getZ());
        return polygon(lowestXZ, highestXLowestZ, highestXZ, lowestXHighestZ);
    }

    /**
     * A square.
     *
     * @param center
     *            Center of the square.
     * @param halfWidth
     *            How many pixels the square extends in each direction.
     * @return The square.
     */
    public static Polygon square(final MapLocation center, final int halfWidth) {
        final MapLocation lowest = MapLocation.of(center.getX() - halfWidth, center.getZ() - halfWidth);
        final MapLocation highest = MapLocation.of(center.getX() + halfWidth, center.getZ() + halfWidth);
        return rectangle(lowest, highest);
    }

    protected final String method;
    /**
     * May not be mutated to keep this class thread-safe and safe for storage.
     */
    protected final JsonElement firstParam;
    /**
     * May not be mutated to keep this class thread-safe and safe for storage.
     */
    protected final JsonObject secondParam;
    protected final HtmlString tooltipOrNull;

    Marker(String method, JsonElement firstParam, JsonObject secondParam, HtmlString tooltipOrNull) {
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
     * {@inheritDoc} This method is thread-safe.
     */
    @Override
    public JsonElement toJsonElement() {
        JsonObject object = new JsonObject();
        object.addProperty("method", method);
        object.add("firstParam", firstParam);
        object.add("secondParam", secondParam);
        if (this.tooltipOrNull != null) {
            object.addProperty("tooltip", tooltipOrNull.toString());
        }
        return object;
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

}
