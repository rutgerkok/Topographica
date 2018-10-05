package nl.rutgerkok.topographica.render;

import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_CHUNKS;
import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_CHUNKS_BITS;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import nl.rutgerkok.topographica.config.WorldConfig;
import nl.rutgerkok.topographica.scheduler.Computation;
import nl.rutgerkok.topographica.scheduler.TGRunnable;
import nl.rutgerkok.topographica.util.Region;

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
            super(Type.LONG_RUNNING, "Chunk painter");
            this.image = Objects.requireNonNull(image, "image");
        }

        @Override
        public void run() {
            while (!future.isDone()) {
                try {
                    ChunkTask task = renderQueue.poll(500, TimeUnit.MILLISECONDS);
                    if (task == null) {
                        // This will check future.isDone before trying again
                        continue;
                    }
                    if (task.snapshot == null) {
                        // Done! This is the poison object, so there are no more
                        // regions coming
                        future.set(new DrawnRegion(renderingRegion, image));
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

    /**
     * Final result class.
     */
    static class DrawnRegion {
        final Region region;
        final Canvas canvas;

        public DrawnRegion(Region region, Canvas canvas) {
            this.region = Objects.requireNonNull(region, "region");
            this.canvas = Objects.requireNonNull(canvas, "canvas");
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

        /**
         * If a the code in a tick ran for too long, this variable is set to the
         * amount of nanoseconds that the code ran for too long. The max run
         * time of the next tick is then decremented by this amount.
         */
        private long overBudgetNs = 0;

        public SyncPart() {
            super(Type.EVERY_TICK, "Chunk loader");
        }

        private void markAsFinished() {
            // Mark as completed, stops it from repeating further
            future.set(null);
            // Add poison object to complete the async render task
            renderQueue.add(new ChunkTask());
        }

        private boolean next() {
            while (true) {
                // Keep searching until a suitable chunk is found
                chunkXInRegion++;
                if (chunkXInRegion >= REGION_SIZE_CHUNKS) {
                    chunkXInRegion = 0;
                    chunkZInRegion++;
                    if (chunkZInRegion >= REGION_SIZE_CHUNKS) {
                        // Done!
                        return false;
                    }
                }

                // We have a candidate
                int chunkX = renderingRegion.getRegionX() << REGION_SIZE_CHUNKS_BITS | chunkXInRegion;
                int chunkZ = renderingRegion.getRegionZ() << REGION_SIZE_CHUNKS_BITS | chunkZInRegion;
                if (worldConfig.getRenderArea().shouldRenderChunk(chunkX, chunkZ)) {
                    return true;
                }
            }
        }

        private void offer() {
            int chunkX = renderingRegion.getRegionX() << REGION_SIZE_CHUNKS_BITS | chunkXInRegion;
            int chunkZ = renderingRegion.getRegionZ() << REGION_SIZE_CHUNKS_BITS | chunkZInRegion;

            boolean alreadyLoaded = world.isChunkLoaded(chunkX, chunkZ);
            if (world.loadChunk(chunkX, chunkZ, false)) {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, false, false);
                if (!alreadyLoaded) {
                    chunk.unload(false);
                }
                renderQueue.add(new ChunkTask(snapshot));
            }
        }

        @Override
        public void run() {
            // SyncPart - load the data of as many chunks as possible within the
            // time budget
            long startTime = System.nanoTime();
            long endTime = startTime + maxNsPerTick - overBudgetNs;
            long currentTime = startTime;
            // Load as many chunks as possible
            while (renderQueue.size() < 100 && currentTime - endTime <= 0) {
                offer();
                if (!next()) {
                    markAsFinished();
                    return; // The full computation is finished
                }
                currentTime = System.nanoTime();
            }
            overBudgetNs = currentTime - endTime;
            overBudgetNs /= 1.5;
        }

    }

    private final World world;
    private final Region renderingRegion;
    private final BlockingQueue<ChunkTask> renderQueue = new LinkedBlockingQueue<>();
    private final int maxNsPerTick;
    private final ChunkRenderer chunkRenderer;
    private final WorldConfig worldConfig;

    public RegionRenderer(WorldConfig worldConfig, World world, Region region) {
        this.worldConfig = Objects.requireNonNull(worldConfig, "worldConfig");
        this.world = Objects.requireNonNull(world, "world");
        this.renderingRegion = Objects.requireNonNull(region, "region");

        this.chunkRenderer = ChunkRenderer.create(worldConfig);
        this.maxNsPerTick = worldConfig.getMaxChunkLoadTimeNSPT();
    }

    public Computation<DrawnRegion> getRenderTasks(Canvas image) {
        return new Computation<>(new AsyncPart(image), new SyncPart());
    }

}
