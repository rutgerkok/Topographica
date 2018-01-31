package nl.rutgerkok.topographica.render;

import org.bukkit.Color;

public interface Canvas {

    /**
     * Used by the painter to add a pixel.
     *
     * @param blockXInWorld
     *            The block x.
     * @param blockZInWorld
     *            The block z.
     * @param color
     *            The color.
     */
    void setColor(int blockXInRegion, int blockZInRegion, Color color);

}
