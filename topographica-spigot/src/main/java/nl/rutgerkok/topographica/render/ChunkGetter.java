package nl.rutgerkok.topographica.render;

import java.util.Optional;

import org.bukkit.ChunkSnapshot;

public interface ChunkGetter {

    /**
     * Gets the chunk snapshot for the given chunk coords. Returns an empty
     * value if no chunk exists at that location.
     *
     * @param chunkX
     *            Chunk x.
     * @param chunkZ
     *            Chunk z.
     * @return The chunk snapshot.
     */
    Optional<ChunkSnapshot> getChunk(int chunkX, int chunkZ);
}
