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
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import nl.rutgerkok.topographica.config.Config;
import nl.rutgerkok.topographica.config.WorldConfig;
import nl.rutgerkok.topographica.render.RegionRenderer.DrawnRegion;
import nl.rutgerkok.topographica.scheduler.Computation;
import nl.rutgerkok.topographica.scheduler.ComputationFactory;
import nl.rutgerkok.topographica.util.Region;

import org.bukkit.World;

public class WorldRenderer extends ComputationFactory<DrawnRegion> {

    private final List<Canvas> unusedCanvases = new CopyOnWriteArrayList<>();

    private final World world;
    private final WorldConfig worldConfig;
    private final Path imageFolder;

    /**
     * Never access directly. Use {@link #getRegion()} and
     * {@link #addRegion(Region)}.
     */
    private final LinkedHashSet<Region> regionQueue = new LinkedHashSet<>();

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
        this.worldConfig = config.getConfig(world);
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
                    if (!worldConfig.shouldRender(regionX, regionZ)) {
                        continue;
                    }
                    regionQueue.add(Region.of(regionX, regionZ));
                } catch (NumberFormatException e) {
                    // Not a region file, ignore
                }
            }
        } catch (DirectoryIteratorException e) {
            throw e.getCause();
        }
    }

    /**
     * Adds a region to the render queue. Can be called from any thread.
     *
     * @param region
     *            The region.
     */
    public void addRegion(Region region) {
        synchronized (regionQueue) {
            regionQueue.add(region);
        }
    }

    /**
     * Clears the render queue.
     */
    public void clearRegionQueue() {
        this.regionQueue.clear();
    }

    /**
     * Gets the size of the region queue.
     *
     * @return The size.
     */
    public int getQueueSize() {
        return this.regionQueue.size();
    }

    private Region getRegion() throws NoSuchElementException {
        synchronized (regionQueue) {
            Iterator<Region> iterator = regionQueue.iterator();
            Region region = iterator.next();
            iterator.remove();
            return region;
        }
    }

    private Path getRegionFolder() {
        return world.getWorldFolder().toPath().resolve("region");
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
        Path file = this.imageFolder.resolve("zoom-1")
                .resolve("r." + image.region.getRegionX() + "." + image.region.getRegionZ() + ".jpg");

        image.canvas.outputAndReset(file);
        unusedCanvases.add(image.canvas);
    }

    @Override
    public Computation<DrawnRegion> next() throws NoSuchElementException {
        Region region = getRegion();
        Canvas image = grabImage();
        RegionRenderer renderer = new RegionRenderer(worldConfig, world, region);
        return renderer.getRenderTasks(image);
    }

}
