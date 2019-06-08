package nl.rutgerkok.topographica.marker;

import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Style of a polygon. Note that this class is not thread-safe: you should keep
 * instances in one thread.
 *
 */
public final class PolygonStyle {

    /**
     * Determines what pixels are inside a shape. See <a href=
     * "https://developer.mozilla.org/docs/Web/SVG/Attribute/fill-rule">the
     * MDN</a>.
     *
     */
    enum FillRule {
        /**
         * Fill all pixels that are inside the shape, after calculating the
         * whole shape.
         */
        NON_ZERO,
        /**
         * Draw an (imaginary) line from a pixel to infinity. Check how many
         * borders were crossed. Only if that number is odd, the pixel is
         * filled.
         */
        EVEN_ODD
    }

    /**
     * Creates a style for a polygon with the default colors and strokes
     *
     * @return The style.
     */
    public static PolygonStyle createUsingDefaults() {
        return new PolygonStyle();
    }

    final Map<String, Object> options = new HashMap<>();

    private PolygonStyle() {

    }

    /**
     * Sets whether the shape must be filled. Note: {@link #fillRule(FillRule)}
     * and {@link #fillColor(Color)} will automatically update call this method,
     * so calling this method yourself shouldn't be necessary.
     *
     * @param fill
     *            True if the shape must be filled, false otherwise.
     * @return This, for chaining.
     */
    public PolygonStyle fill(boolean fill) {
        options.put("fill", fill);
        return this;
    }

    /**
     * Sets the color that the shape must be filled with. The alpha value of the
     * color is respected. Note: if no color would be specified (so if this
     * method was not called), {@link #strokeColor(Color)} is used.
     *
     * @param color
     *            The fill color.
     * @return This, for chaining.
     */
    public PolygonStyle fillColor(Color color) {
        fill(color.getAlpha() > 0);
        options.put("fillColor", HtmlColor.getHexString(color));
        options.put("fillOpacity", color.getAlpha() / 256.0);
        return this;
    }

    /**
     * Sets the fill method, which determines what pixels are inside. Default is
     * {@link FillRule#EVEN_ODD}.
     *
     * @param method
     *            The fill method.
     * @return This, for chaining.
     */
    public PolygonStyle fillRule(FillRule method) {
        fill(true);
        options.put("fillRule", method.toString().toLowerCase(Locale.ROOT).replace("_", ""));
        return this;
    }

    /**
     * Sets whether a stroke must be drawn. Note: the
     * {@link #strokeColor(Color)} and {@link #strokeWidth(double)} methods will
     * automatically update this value, so calling this method usually isn't
     * necessary.
     *
     * @param stroke
     *            True if a stroke is drawn, false otherwise.
     * @return This, for chaining.
     */
    public PolygonStyle stroke(boolean stroke) {
        options.put("stroke", stroke);
        return this;
    }

    /**
     * Sets the color of the stroke. The alpha value of the color is respected.
     *
     * @param color
     *            The stroke color.
     * @return This, for chaining.
     */
    public PolygonStyle strokeColor(Color color) {
        stroke(color.getAlpha() > 0);
        options.put("color", HtmlColor.getHexString(color));
        options.put("opacity", color.getAlpha() / 256.0);
        return this;
    }

    /**
     * Sets the width of the stroke.
     *
     * @param pixels
     *            The stroke width, in pixels.
     * @return This, for chaining.
     */
    public PolygonStyle strokeWidth(double pixels) {
        if (pixels < 0) {
            throw new IllegalArgumentException("Negative stroke width: " + pixels);
        }
        stroke(pixels > 0);
        options.put("weight", pixels);
        return this;
    }
}
