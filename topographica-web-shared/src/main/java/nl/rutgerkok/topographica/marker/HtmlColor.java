package nl.rutgerkok.topographica.marker;

import java.awt.Color;

public class HtmlColor {

    /**
     * Gets a HEX color string (like "#0134ab") from the Java color.
     *
     * @param color
     *            The Java color.
     * @return The HEX string.
     */
    public static String getHexString(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private HtmlColor() {
        // No instances
    }
}
