package nl.rutgerkok.topographica.util;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.bukkit.ChunkSnapshot;

/**
 * Provides access to the server thread.
 */
public interface ChunkSnapshotGetter {

    /**
     * The result of getting a chunk snapshot.
     *
     */
    public static class ChunkResult {
        public final Optional<ChunkSnapshot> snapshot;
        /**
         * True if it was necessary to load a chunk from disk.
         */
        public final boolean alreadyLoaded;

        public ChunkResult(Optional<ChunkSnapshot> snapshot, boolean neededToLoadChunk) {
            this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
            this.alreadyLoaded = neededToLoadChunk;
        }
    }

    /**
     * Runs the given code on the server thread, and returns the result.
     *
     * @param callable
     *            The code.
     * @return The result.
     */
    Future<ChunkResult> runOnServerThread(Callable<ChunkResult> callable);
}
