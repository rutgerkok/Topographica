package nl.rutgerkok.topographica;

import nl.rutgerkok.topographica.config.ColorConfig;
import nl.rutgerkok.topographica.render.ChunkRenderer;

public class VersionSpecific {

    /**
     * Creates a chunk renderer.
     *
     * @param config
     *            The colors.
     * @return The renderer.
     */
    public static ChunkRenderer createChunkRenderer(ColorConfig config) {
        throw new UnsupportedOperationException();
    }
}
