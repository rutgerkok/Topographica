package nl.rutgerkok.topographica.render;

import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_CHUNKS;
import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_CHUNKS_BITS;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public final class RegionRenderer {

    /**
     * Runs on another thread, grabs as many chunks to render as it can.
     *
     */
    private class AsyncPart extends BukkitRunnable {

        private final Canvas image;
        private final ChunkRenderer chunkRenderer = new ChunkRenderer();
        private final SettableFuture<Canvas> future;

        public AsyncPart(Canvas image, SettableFuture<Canvas> future) {
            this.image = Objects.requireNonNull(image, "image");
            this.future = Objects.requireNonNull(future, "future");
        }

        @Override
        public void run() {
            while (!isCancelled()) {
                try {
                    ChunkTask task = renderQueue.take();
                    if (task.snapshot == null) {
                        // Done!
                        future.set(image);
                        return; // Ends task
                    } else {
                        chunkRenderer.render(task.snapshot, image);
                    }
                } catch (InterruptedException e) {
                    future.setException(e);
                    return;
                }
            }
            future.setException(new RuntimeException("Cancelled"));
        }
    }

    private static final class ChunkTask {
        final ChunkSnapshot snapshot;

        /**
         * Used when there will be no more snapshots.
         */
        ChunkTask() {
            this.snapshot = null;
        }
        
        ChunkTask(ChunkSnapshot snapshot) {
            this.snapshot = Objects.requireNonNull(snapshot);
        }
    }

    /**
     * Runs on the main thread, makes sure new chunks are always available for
     * the region renderer until the last region has been rendered.
     *
     */
    private class SyncPart extends BukkitRunnable {

        private int chunkXInRegion;
        private int chunkZInRegion;

        private void markAsFinished() {
            cancel(); // Stop this task from repeating further
            renderQueue.add(new ChunkTask()); // Add poison object to stop the async render task
        }

        private boolean next() {
            chunkXInRegion++;
            if (chunkXInRegion >= REGION_SIZE_CHUNKS) {
                chunkXInRegion = 0;
                chunkZInRegion++;
                if (chunkZInRegion >= REGION_SIZE_CHUNKS) {
                    // Done!
                    markAsFinished();
                    return false;
                }
            }
            return true;
        }

        private void offer() {
            int chunkX = regionX << REGION_SIZE_CHUNKS_BITS | chunkXInRegion;
            int chunkZ = regionZ << REGION_SIZE_CHUNKS_BITS | chunkZInRegion;

            boolean alreadyLoaded = world.isChunkLoaded(chunkX, chunkZ);
            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            if (chunk.load(false)) {
                ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, false, false);
                if (!alreadyLoaded) {
                    chunk.unload(false);
                }
                renderQueue.add(new ChunkTask(snapshot));
            }
        }

        @Override
        public void run() {
            if (renderQueue.size() > 1) {
                // Queue is too full
                return;
            }
            for (int i = 0; i < 2; i++) {
                offer();
                if (!next()) {
                    return;
                }
            }
        }

    }

    private final World world;
    private final int regionX;
    private final int regionZ;
    private final BlockingQueue<ChunkTask> renderQueue = new LinkedBlockingQueue<>();

    public RegionRenderer(World world, int regionX, int regionZ) {
        this.world = Objects.requireNonNull(world);
        this.regionX = regionX;
        this.regionZ = regionZ;
    }

    public ListenableFuture<Canvas> render(Plugin plugin, Canvas image) {
        SettableFuture<Canvas> settableFuture = SettableFuture.create();
        new AsyncPart(image, settableFuture).runTaskAsynchronously(plugin);
        new SyncPart().runTaskTimer(plugin, 0, 1);
        return settableFuture;
    }

}
