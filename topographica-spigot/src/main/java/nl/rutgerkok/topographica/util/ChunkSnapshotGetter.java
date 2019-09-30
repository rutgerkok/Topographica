package nl.rutgerkok.topographica.util;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

/**
 * Provides access to the server thread.
 */
public class ChunkSnapshotGetter {

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

        /**
         * The amount of players online, which can be used to decide how fast
         * the next chunk is going to be loaded.
         */
        public final int playersOnline;

        public ChunkResult(Optional<ChunkSnapshot> snapshot, int playersOnline, boolean neededToLoadChunk) {
            this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
            this.playersOnline = playersOnline;
            this.alreadyLoaded = neededToLoadChunk;
        }
    }

    private final Plugin plugin;

    public ChunkSnapshotGetter(Plugin plugin) {
        super();
        this.plugin = plugin;
    }

    /**
     * Gets the chunk data. Method must be called on an asynchronous thread.
     *
     * @param world
     *            The world.
     * @param chunkX
     *            Chunk x.
     * @param chunkZ
     *            Chunk z.
     * @return The result.
     */
    public Future<ChunkResult> getChunk(World world, int chunkX, int chunkZ) {
        Callable<ChunkResult> callable = () -> {
            int playersOnline = plugin.getServer().getOnlinePlayers().size();

            // We need to get chunks on the server threads
            boolean alreadyLoaded = world.isChunkLoaded(chunkX, chunkZ);

            if (world.loadChunk(chunkX, chunkZ, false)) {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, false, false);
                if (!alreadyLoaded) {
                    world.unloadChunkRequest(chunkX, chunkZ);
                }
                return new ChunkResult(Optional.of(snapshot), playersOnline, alreadyLoaded);
            }
            return new ChunkResult(Optional.empty(), playersOnline, false);
        };

        return plugin.getServer().getScheduler().callSyncMethod(plugin, callable);
    }
}
