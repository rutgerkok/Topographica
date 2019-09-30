package nl.rutgerkok.topographica.marker;

import java.awt.Color;
import java.util.Locale;

import com.google.gson.JsonObject;

public class LineStyle {
    /**
     * Ending style of a line. See <a href=
     * "https://developer.mozilla.org/en/docs/Web/SVG/Attribute/stroke-linecap">the
     * MDN</a> for more information.
     *
     */
    enum LineCap {
        /**
         * Line ends abruptly.
         */
        BUTT,
        /**
         * Line ends in a circle; center of circle is placed at the line end.
         */
        ROUND,
        /**
         * Line ends with a square; center of square is placed at the line end.
         */
        SQUARE
    }

    /**
     * Describes how to join different line segments. See <a href=
     * "https://developer.mozilla.org/en/docs/Web/SVG/Attribute/stroke-linejoin">the
     * MDN</a> for more information.
     *
     */
    enum LineJoin {
        ARCS,
        BEVEL,
        MITER,
        MITER_CLIP,
        ROUND
    }

    /**
     * Creates a style for a line with the default color and width.
     *
     * @return The style.
     */
    public static LineStyle createUsingDefaults() {
        return new LineStyle();
    }

    final JsonObject options = new JsonObject();

    private LineStyle() {

    }

    /**
     * Sets the color that the line must be filled with. The alpha value of the
     * color is respected.
     *
     * @param color
     *            The line color.
     * @return This, for chaining.
     */
    public LineStyle color(Color color) {
        options.addProperty("color", HtmlColor.getHexString(color));
        options.addProperty("opacity", color.getAlpha() / 256.0);
        return this;
    }

    /**
     * Sets the style used at the end of the line. {@link LineCap#ROUND} is the
     * default.
     *
     * @param lineCap
     *            The style.
     * @return This, for chaining.
     */
    public LineStyle lineCap(LineCap lineCap) {
        options.addProperty("lineCap", lineCap.name().toLowerCase(Locale.ROOT));
        return this;
    }

    /**
     * Sets the style used to join different line segments.
     * {@link LineJoin#ROUND} is the default.
     *
     * @param lineJoin
     *            The style.
     * @return This, for chaining.
     */
    public LineStyle lineJoin(LineJoin lineJoin) {
        options.addProperty("lineJoin", lineJoin.name().toLowerCase(Locale.ROOT).replace('_', '-'));
        return this;
    }

    /**
     * Sets the weight (the line width).
     *
     * @param width
     *            The new line width.
     * @return This, for chaining.
     * @throws IllegalArgumentException
     *             If the width is not positive.
     */
    public LineStyle weight(double width) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be positive, was " + width);
        }
        options.addProperty("weight", width);
        return this;
    }
}
