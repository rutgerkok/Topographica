package nl.rutgerkok.topographica.render;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import nl.rutgerkok.topographica.config.RenderAreaConfig;
import nl.rutgerkok.topographica.util.ConcurrentHashSet;
import nl.rutgerkok.topographica.util.Coordinate;
import nl.rutgerkok.topographica.util.Region;
import nl.rutgerkok.topographica.util.SizeConstants;

public class WorldTaskList {

    public interface DrawContext {

        /**
         * Called to draw a single chunk on the canvas.
         *
         * @param canvas
         *            The canvas.
         * @param chunkX
         *            Chunk x.
         * @param chunkZ
         *            Chunk z.
         */
        void drawChunk(Canvas canvas, int chunkX, int chunkZ);

        /**
         * Gets the save location for the image of the given coords.
         *
         * @param scaledCoords
         *            The coordinates.
         * @param zoomLevel
         *            The zoom level.
         * @return The save location.
         */
        Path getSaveFile(Coordinate scaledCoords, int zoomLevel);

        /**
         * Returns true if the server is stopping, and the plugin therefore
         * needs to halt.
         *
         * @return True if the server is stopping.
         */
        boolean mustStop();
    }

    /**
     * Represents something that can be added to the world task list.
     *
     */
    public static class DrawInstruction {
        public static DrawInstruction ofChunk(int chunkX, int chunkZ) {
            return new DrawInstruction(false, chunkX, chunkZ);
        }

        public static DrawInstruction ofRegion(Region region) {
            return new DrawInstruction(true, region.getRegionX(), region.getRegionZ());
        }

        public final boolean isRegion;

        public final int x;

        public final int z;

        private DrawInstruction(boolean isRegion, int x, int z) {
            this.isRegion = isRegion;
            this.x = x;
            this.z = z;
        }

        int getRegionX() {
            if (isRegion) {
                return x;
            } else {
                return x >> 5;
            }
        }

        int getRegionZ() {
            if (isRegion) {
                return z;
            } else {
                return z >> 5;
            }
        }
    }

    private interface RegionTaskTree {

        /**
         * Calculates the amount of different regions that are currently in the
         * queue.
         *
         * @return The amount of regions.
         */
        int calculateRegionsInQueue();

        /**
         * Draws everything that's currently queued.
         *
         * @param context
         *            Drawing context.
         * @throws IOException
         *             If writing to a file fails.
         */
        void drawAll(DrawContext context) throws IOException;

        /**
         * Adds all pending tasks to the given list.
         *
         * @param list
         *            The list.
         */
        void getQueueSnapshot(List<DrawInstruction> list);

        /**
         * Quickly checks if this task list is empty.
         *
         * @return True if it's empty, false otherwise.
         */
        boolean isEmpty();

        /**
         * Adds a task to redraw the given chunk or region.
         *
         * @param drawInstruction
         *            The thing to redraw.
         */
        void requestRedraw(DrawInstruction drawInstruction);

    }

    /**
     * Controls drawing a single region.
     */
    static class SingleRegionTaskTree implements RegionTaskTree {

        private final Coordinate regionCoord;
        private final RenderAreaConfig renderArea;

        private final Set<Coordinate> chunks = ConcurrentHashSet.create();
        private volatile boolean drawAll = false;

        public SingleRegionTaskTree(RenderAreaConfig renderArea, int regionX, int regionZ) {
            this.renderArea = Objects.requireNonNull(renderArea, "renderArea");
            this.regionCoord = new Coordinate(regionX, regionZ);
        }

        @Override
        public int calculateRegionsInQueue() {
            if (!drawAll && chunks.isEmpty()) {
                return 0;
            }
            return 1;
        }

        @Override
        public void drawAll(DrawContext context) throws IOException {
            Path saveFile = context.getSaveFile(regionCoord, 1);
            Canvas canvas = Canvas.createFromFile(saveFile);
            if (drawAll) {
                // Drawing all chunks in the region
                int regionStartChunkX = regionCoord.x << 5;
                int regionStartChunkZ = regionCoord.z << 5;
                for (int localChunkX = 0; localChunkX < 32; localChunkX++) {
                    for (int localChunkZ = 0; localChunkZ < 32; localChunkZ++) {
                        if (context.mustStop()) {
                            return;
                        }
                        int chunkX = regionStartChunkX + localChunkX;
                        int chunkZ = regionStartChunkZ + localChunkZ;
                        if (!renderArea.shouldRenderChunk(chunkX, chunkZ)) {
                            continue;
                        }
                        context.drawChunk(canvas, chunkX, chunkZ);
                    }
                }
                drawAll = false;
            } else {
                // Drawing only the specified chunks from the region
                for (Iterator<Coordinate> it = chunks.iterator(); it.hasNext();) {
                    if (context.mustStop()) {
                        return;
                    }
                    Coordinate chunk = it.next();
                    context.drawChunk(canvas, chunk.x, chunk.z);
                    it.remove();
                }
            }

            if (!context.mustStop()) {
                // Only write if task hasn't been aborted
                canvas.writeToFile(saveFile);
            }
        }

        @Override
        public void getQueueSnapshot(List<DrawInstruction> list) {
            if (this.drawAll) {
                list.add(DrawInstruction.ofRegion(Region.of(this.regionCoord.x, this.regionCoord.z)));
            } else {
                for (Coordinate chunk : chunks) {
                    list.add(DrawInstruction.ofChunk(chunk.x, chunk.z));
                }
            }
        }

        @Override
        public boolean isEmpty() {
            return !drawAll && chunks.isEmpty();
        }

        private void requestChunkRedraw(int chunkX, int chunkZ) {
            if (!this.drawAll) {
                chunks.add(new Coordinate(chunkX, chunkZ));
            }
        }

        @Override
        public void requestRedraw(DrawInstruction drawInstruction) {
            if (drawInstruction.isRegion) {
                requestRegionRedraw(drawInstruction.x, drawInstruction.z);
            } else {
                requestChunkRedraw(drawInstruction.x, drawInstruction.z);
            }
        }

        private void requestRegionRedraw(int regionX, int regionZ) {
            if (regionX != this.regionCoord.x || regionZ != this.regionCoord.z) {
                throw new IllegalArgumentException("Wrong region: " + regionX + "," + regionZ);
            }
            this.drawAll = true;
            this.chunks.clear();
        }
    }

    private static class SuperRegionTaskTree implements RegionTaskTree {
        private final Map<Coordinate, RegionTaskTree> children = new ConcurrentHashMap<>();

        /**
         * Zoom level, from 1 to infinity.
         */
        protected final int zoomLevel;

        /**
         * The area of the world that must be rendered.
         */
        private final RenderAreaConfig renderArea;

        private SuperRegionTaskTree(RenderAreaConfig renderArea, int zoomLevel) {
            if (zoomLevel <= 0) {
                throw new IllegalArgumentException("Invalid zoomLevel: " + zoomLevel);
            }
            this.zoomLevel = zoomLevel;
            this.renderArea = Objects.requireNonNull(renderArea, "renderArea");
        }

        @Override
        public int calculateRegionsInQueue() {
            int sum = 0;
            for (RegionTaskTree tree : this.children.values()) {
                sum += tree.calculateRegionsInQueue();
            }
            return sum;
        }

        private RegionTaskTree createChild(Coordinate regionLocation) {
            int nextZoomLevel = zoomLevel - 1;
            if (nextZoomLevel == 1) {
                // No super region needed for next zoom, it's just a single
                // region now
                return new SingleRegionTaskTree(renderArea, regionLocation.x, regionLocation.z);
            }
            return new ZoomingRegionTaskTree(renderArea, regionLocation, nextZoomLevel);
        }

        @Override
        public void drawAll(DrawContext context) throws IOException {
            for (Iterator<RegionTaskTree> it = this.children.values().iterator(); it.hasNext();) {
                RegionTaskTree tree = it.next();
                if (context.mustStop()) {
                    return;
                }
                tree.drawAll(context);
                if (context.mustStop()) {
                    return;
                }

                if (tree.isEmpty()) {
                    // If it's still empty (read: no other thread has added new
                    // tasks in between),
                    // remove it.
                    it.remove();
                }
            }

        }

        @Override
        public void getQueueSnapshot(List<DrawInstruction> list) {
            // Ask the children to see which regions are in the queue
            for (RegionTaskTree child : this.children.values()) {
                child.getQueueSnapshot(list);
            }
        }

        @Override
        public boolean isEmpty() {
            return children.isEmpty();
        }

        @Override
        public void requestRedraw(DrawInstruction drawInstruction) {
            int regionX = drawInstruction.getRegionX();
            int regionZ = drawInstruction.getRegionZ();
            int superRegionX = regionX >> (zoomLevel - 2);
            int superRegionZ = regionZ >> (zoomLevel - 2);
            children.computeIfAbsent(new Coordinate(superRegionX, superRegionZ), this::createChild)
                    .requestRedraw(drawInstruction);
        }

    }

    private static class ZoomingRegionTaskTree extends SuperRegionTaskTree {

        /**
         * Scaled coordinate.
         */
        private final Coordinate coord;

        private ZoomingRegionTaskTree(RenderAreaConfig renderArea, Coordinate coord, int zoomLevel) {
            super(renderArea, zoomLevel);
            this.coord = Objects.requireNonNull(coord, "coord");
        }

        @Override
        public void drawAll(DrawContext context) throws IOException {
            super.drawAll(context);

            // Draw zoomed out image
            if (!context.mustStop()) {
                drawZoomedOutImage(context);
            }
        }

        private void drawZoomedOutImage(DrawContext context) throws IOException {
            BufferedImage zoomedOut = new BufferedImage(SizeConstants.REGION_SIZE_PIXELS,
                    SizeConstants.REGION_SIZE_PIXELS, BufferedImage.TYPE_INT_RGB);
            Graphics2D zoomedOutGraphics = zoomedOut.createGraphics();
            boolean successful = false;
            try {

                BufferedImage subImage;

                // Top left
                Coordinate coordinate = new Coordinate(this.coord.x * 2, this.coord.z * 2);
                try {
                    subImage = ImageIO.read(context.getSaveFile(coordinate, zoomLevel - 1).toFile());
                    zoomedOutGraphics.drawImage(subImage, 0, 0, SizeConstants.REGION_SIZE_PIXELS / 2,
                            SizeConstants.REGION_SIZE_PIXELS / 2,
                            0, 0, SizeConstants.REGION_SIZE_PIXELS, SizeConstants.REGION_SIZE_PIXELS, null);
                } catch (IOException e) {
                    // Ignore
                }

                // Top right
                coordinate = new Coordinate(coordinate.x + 1, coordinate.z);
                try {
                    subImage = ImageIO.read(context.getSaveFile(coordinate, zoomLevel - 1).toFile());
                    zoomedOutGraphics.drawImage(subImage, SizeConstants.REGION_SIZE_PIXELS / 2, 0,
                            SizeConstants.REGION_SIZE_PIXELS,
                            SizeConstants.REGION_SIZE_PIXELS / 2,
                            0, 0, SizeConstants.REGION_SIZE_PIXELS, SizeConstants.REGION_SIZE_PIXELS, null);
                } catch (IOException e) {
                    // Ignore
                }

                // Bottom right
                coordinate = new Coordinate(coordinate.x, coordinate.z + 1);
                try {
                    subImage = ImageIO.read(context.getSaveFile(coordinate, zoomLevel - 1).toFile());
                    zoomedOutGraphics.drawImage(subImage, SizeConstants.REGION_SIZE_PIXELS / 2,
                            SizeConstants.REGION_SIZE_PIXELS / 2, SizeConstants.REGION_SIZE_PIXELS,
                            SizeConstants.REGION_SIZE_PIXELS, 0, 0, SizeConstants.REGION_SIZE_PIXELS,
                            SizeConstants.REGION_SIZE_PIXELS, null);
                } catch (IOException e) {
                    // Ignore
                }

                // Bottom left
                coordinate = new Coordinate(coordinate.x - 1, coordinate.z);
                try {
                    subImage = ImageIO.read(context.getSaveFile(coordinate, zoomLevel - 1).toFile());
                    zoomedOutGraphics.drawImage(subImage, 0,
                            SizeConstants.REGION_SIZE_PIXELS / 2, SizeConstants.REGION_SIZE_PIXELS / 2,
                            SizeConstants.REGION_SIZE_PIXELS, 0, 0, SizeConstants.REGION_SIZE_PIXELS,
                            SizeConstants.REGION_SIZE_PIXELS, null);
                } catch (IOException e) {
                    // Ignore
                }
                successful = true;
            } finally {
                // Dispose graphics, write result to file
                zoomedOutGraphics.dispose();
                if (successful) {
                    Path saveFile = context.getSaveFile(coord, zoomLevel);
                    Files.createDirectories(saveFile.getParent());
                    ImageIO.write(zoomedOut, "PNG", saveFile.toFile());
                }
            }
        }
    }

    private final SuperRegionTaskTree root;
    private final RenderAreaConfig renderArea;

    public WorldTaskList(RenderAreaConfig renderArea) {
        this.renderArea = Objects.requireNonNull(renderArea, "renderArea");
        this.root = new SuperRegionTaskTree(renderArea, 5);
    }

    /**
     * Gets the amount of different regions in this queue.
     *
     * @return The amount of regions.
     */
    public int calculateRegionsInQueue() {
        return root.calculateRegionsInQueue();
    }

    void drawAll(DrawContext context) throws IOException {
        root.drawAll(context);
    }

    /**
     * Gets everything that's currently in the queue.
     *
     * @return Everything that's in the queue.
     */
    public List<DrawInstruction> getQueueSnapshot() {
        List<DrawInstruction> list = new ArrayList<>();
        root.getQueueSnapshot(list);
        return Collections.unmodifiableList(list);
    }

    /**
     * Puts in a request to (re)draw a single chunk or region. Other things
     * should not be (re)drawn.
     *
     * @param drawInstruction
     *            The thing to redraw.
     */
    void requestRedraw(DrawInstruction drawInstruction) {
        if (drawInstruction.isRegion
                && !renderArea.shouldRenderRegion(drawInstruction.x, drawInstruction.z)) {
            return;
        }
        if (!drawInstruction.isRegion
                && !renderArea.shouldRenderChunk(drawInstruction.x, drawInstruction.z)) {
            return;
        }
        root.requestRedraw(drawInstruction);
    }

}
