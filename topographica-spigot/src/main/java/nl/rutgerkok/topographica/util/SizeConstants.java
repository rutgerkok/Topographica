package nl.rutgerkok.topographica.util;

/**
 * A bunch of constants related to the size of regions, chunks, pixels and
 * blocks.
 */
public final class SizeConstants {
    public static final int CHUNK_SIZE_BLOCKS_BITS = 4;
    public static final int CHUNK_SIZE_BLOCKS = 1 << CHUNK_SIZE_BLOCKS_BITS;
    public static final int CHUNK_HEIGHT_BLOCKS = 256;

    /**
     * The level at which cave maps start rendering.
     */
    public static final int NETHER_START_BLOCK_Y = 63;
    /**
     * The highest level at which cave maps generate blocks. This should be
     * bedrock.
     */
    public static final int NETHER_HIGHEST_BLOCK_Y = 127;

    public static final int PIXEL_SIZE_BLOCKS_BITS = 1;
    public static final int PIXEL_SIZE_BLOCKS = 1 << PIXEL_SIZE_BLOCKS_BITS;

    public static final int REGION_SIZE_BLOCKS_BITS = 9;
    public static final int REGION_SIZE_BLOCKS = 1 << REGION_SIZE_BLOCKS_BITS;

    public static final int REGION_SIZE_PIXELS_BITS = REGION_SIZE_BLOCKS_BITS - PIXEL_SIZE_BLOCKS_BITS;
    public static final int REGION_SIZE_PIXELS = 1 << REGION_SIZE_PIXELS_BITS;

    public static final int REGION_SIZE_CHUNKS_BITS = REGION_SIZE_BLOCKS_BITS - CHUNK_SIZE_BLOCKS_BITS;
    public static final int REGION_SIZE_CHUNKS = 1 << REGION_SIZE_CHUNKS_BITS;
}
