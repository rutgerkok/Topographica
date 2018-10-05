package nl.rutgerkok.topographica.render;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import nl.rutgerkok.topographica.config.Config;
import nl.rutgerkok.topographica.config.WorldConfig;
import nl.rutgerkok.topographica.render.RegionRenderer.DrawnRegion;
import nl.rutgerkok.topographica.scheduler.Computation;
import nl.rutgerkok.topographica.scheduler.ComputationFactory;
import nl.rutgerkok.topographica.util.ConcurrentHashSet;
import nl.rutgerkok.topographica.util.Region;

import org.bukkit.World;
import org.bukkit.block.Block;

public class WorldRenderer extends ComputationFactory<DrawnRegion> {

    private final List<Canvas> unusedCanvases = new CopyOnWriteArrayList<>();

    private final World world;
    private final WorldConfig worldConfig;
    private final Path imageFolder;

    /**
     * Never access directly. Use {@link #getRegionForRendering()} and
     * {@link #addRegionForced(Region)}.
     */
    private final LinkedHashSet<Region> regionQueue = new LinkedHashSet<>();

    private final Set<Region> currentlyRendering = ConcurrentHashSet.create();

    /**
     * Creates a renderer for the full world.
     *
     * @param world
     *            The world to render.
     * @param config
     *            Plugin configuration.
     */
    public WorldRenderer(World world, Config config) {
        this.world = Objects.requireNonNull(world, "world");
        this.worldConfig = config.getWorldConfig(world);
        this.imageFolder = config.getWebConfig().getImagesFolder().resolve(world.getName());
    }

    /**
     * Adds all regions of a world (that fall within the render limits as
     * specified by the config setting) to the render queue. This method can be
     * called from any thread, but as it is expected to take a while to run on
     * larger worlds, it is best to call it from an async thread.
     *
     * @throws IOException
     *             If reading the directory fails.
     */
    public void addAllRegions() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(getRegionFolder(), "*.mca")) {
            for (Path file : stream) {
                String[] fileName = file.getFileName().toString().split("\\.");
                if (fileName.length != 4 || !fileName[0].equals("r")) {
                    continue;
                }
                try {
                    int regionX = Integer.parseInt(fileName[1]);
                    int regionZ = Integer.parseInt(fileName[2]);
                    Region region = Region.of(regionX, regionZ);
                    if (!worldConfig.getRenderArea().shouldRenderRegion(region)) {
                        continue;
                    }
                    addRegionForced(region);
                } catch (NumberFormatException e) {
                    // Not a region file, ignore
                }
            }
        } catch (DirectoryIteratorException e) {
            throw e.getCause();
        }
    }

    /**
     * Adds a region to the render queue, even if it shouldn't be rendered
     * according to the world settings. Can be called from any thread.
     *
     * @param region
     *            The region.
     */
    private void addRegionForced(Region region) {
        synchronized (regionQueue) {
            regionQueue.add(region);
        }
    }

    /**
     * Clears the render queue.
     */
    public void clearRegionQueue() {
        synchronized (regionQueue) {
            this.regionQueue.clear();
        }
    }

    /**
     * Gets the size of the region queue.
     *
     * @return The size.
     */
    public int getQueueSize() {
        synchronized (regionQueue) {
            return this.regionQueue.size() + this.currentlyRendering.size();
        }
    }

    /**
     * Gets a snapshot of the rendering queue.
     *
     * @return The snapshot.
     */
    public Set<Region> getQueueSnapshot() {
        Set<Region> result = new LinkedHashSet<>(); // Keep insertion order
        synchronized (this.regionQueue) {
            result.addAll(regionQueue);
            result.addAll(currentlyRendering);
        }
        return result;
    }

    private Path getRegionFolder() {
        return world.getWorldFolder().toPath().resolve("region");
    }

    private Region getRegionForRendering() throws NoSuchElementException {
        synchronized (regionQueue) {
            Iterator<Region> iterator = regionQueue.iterator();
            Region region = iterator.next();
            iterator.remove();
            this.currentlyRendering.add(region);
            return region;
        }
    }

    @Override
    public UUID getUniqueId() {
        return world.getUID();
    }

    /**
     * Gets the world this renderer renders for.
     *
     * @return The world.
     */
    public World getWorld() {
        return world;
    }

    /**
     * Grabs an unused image, or creates a new one if none available.
     *
     * @return The image.
     */
    private Canvas grabImage() {
        try {
            return unusedCanvases.remove(0);
        } catch (IndexOutOfBoundsException e) {
            // No unused images - create new one
            return new Canvas();
        }
    }

    @Override
    public void handleResult(DrawnRegion image) throws IOException {
        try {
            Path file = this.imageFolder.resolve("zoom-1")
                    .resolve("r." + image.region.getRegionX() + "." + image.region.getRegionZ() + ".jpg");

            image.canvas.outputAndReset(file);
        } finally {
            unusedCanvases.add(image.canvas);
            currentlyRendering.remove(image.region);
        }
    }

    @Override
    public Computation<DrawnRegion> next() throws NoSuchElementException {
        Region region = getRegionForRendering();

        Canvas image = grabImage();
        RegionRenderer renderer = new RegionRenderer(worldConfig, world, region);
        return renderer.getRenderTasks(image);
    }

    /**
     * Renders a region of the block at some point in the future, but only if
     * the block falls within the bounds of the world that are rendered.
     *
     * @param block
     *            The block.
     * @return True if the region will be rendered, false otherwise.
     */
    public boolean tryAddBlock(Block block) {
        if (this.worldConfig.getRenderArea().shouldRenderColumn(block.getX(), block.getZ())) {
            this.addRegionForced(Region.ofBlock(block));
            return true;
        }
        return false;
    }

    /**
     * Renders a region at some point in the future, but only if it falls within
     * the bounds of the world that are rendered.
     *
     * @param region
     *            The region.
     * @return True if the region will be rendered, false otherwise.
     */
    public boolean tryAddRegion(Region region) {
        if (this.worldConfig.getRenderArea().shouldRenderRegion(region)) {
            this.addRegionForced(region);
            return true;
        }
        return false;
    }

}
