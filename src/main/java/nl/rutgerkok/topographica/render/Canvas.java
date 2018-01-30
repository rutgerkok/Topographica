package nl.rutgerkok.topographica.render;

import org.bukkit.Color;

public interface Canvas {

    final int CHUNK_SIZE_BLOCKS_BITS = 4;
    final int CHUNK_SIZE_BLOCKS = 1 << CHUNK_SIZE_BLOCKS_BITS;

    final int PIXEL_SIZE_BLOCKS_BITS = 1;
    final int PIXEL_SIZE_BLOCKS = 1 << PIXEL_SIZE_BLOCKS_BITS;

    final int REGION_SIZE_BLOCKS_BITS = 9;
    final int REGION_SIZE_BLOCKS = 1 << REGION_SIZE_BLOCKS_BITS;

    final int REGION_SIZE_PIXELS_BITS = REGION_SIZE_BLOCKS_BITS - PIXEL_SIZE_BLOCKS_BITS;
    final int REGION_SIZE_PIXELS = 1 << REGION_SIZE_PIXELS_BITS;

    final int REGION_SIZE_CHUNKS_BITS = REGION_SIZE_BLOCKS_BITS - CHUNK_SIZE_BLOCKS_BITS;
    final int REGION_SIZE_CHUNKS = 1 << REGION_SIZE_CHUNKS_BITS;

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