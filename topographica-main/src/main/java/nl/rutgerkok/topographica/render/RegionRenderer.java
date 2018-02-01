package nl.rutgerkok.topographica.render;

import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_CHUNKS;
import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_CHUNKS_BITS;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import nl.rutgerkok.topographica.config.WorldConfig;
import nl.rutgerkok.topographica.scheduler.Computation;
import nl.rutgerkok.topographica.scheduler.TGRunnable;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;

public final class RegionRenderer {

    /**
     * Runs on another thread, grabs as many chunks to render as it can.
     *
     */
    private class AsyncPart extends TGRunnable<DrawnRegion> {

        private final Canvas image;

        public AsyncPart(Canvas image) {
            super(Type.LONG_RUNNING);
            this.image = Objects.requireNonNull(image, "image");
        }

        @Override
        public void run() {
            while (!future.isCancelled()) {
                try {
                    ChunkTask task = renderQueue.take();
                    if (task.snapshot == null) {
                        // Done!
                        future.set(new DrawnRegion(regionX, regionZ, image));
                        return; // Ends task
                    } else {
                        chunkRenderer.render(task.snapshot, image);
                    }
                } catch (InterruptedException e) {
                    future.setException(e);
                    return;
                }
            }
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

    static class DrawnRegion {
        final int regionX;
        final int regionZ;
        final Canvas canvas;

        public DrawnRegion(int regionX, int regionZ, Canvas canvas) {
            this.regionX = regionX;
            this.regionZ = regionZ;
            this.canvas = Objects.requireNonNull(canvas);
        }
    }

    /**
     * Runs on the main thread, makes sure new chunks are always available for
     * the region renderer until the last region has been rendered.
     *
     */
    private class SyncPart extends TGRunnable<Void> {

        private int chunkXInRegion = 0;
        private int chunkZInRegion = 0;

        public SyncPart() {
            super(Type.EVERY_TICK);
        }

        private void markAsFinished() {
            // Mark as completed, stops it from repeating further
            future.set(null);
            // Add poison object to complete the async render task
            renderQueue.add(new ChunkTask());
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

    private final ChunkRenderer chunkRenderer;

    public RegionRenderer(WorldConfig worldConfig, World world, int regionX, int regionZ) {
        this.chunkRenderer = ChunkRenderer.create(worldConfig.getColors());
        this.world = Objects.requireNonNull(world, "world");
        this.regionX = regionX;
        this.regionZ = regionZ;
    }

    public Computation<DrawnRegion> getRenderTasks(Canvas image) {
        return new Computation<>(new AsyncPart(image), new SyncPart());
    }

}
