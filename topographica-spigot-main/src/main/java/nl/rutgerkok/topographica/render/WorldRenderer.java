package nl.rutgerkok.topographica.render;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.World;

import nl.rutgerkok.topographica.config.Config;
import nl.rutgerkok.topographica.config.WorldConfig;
import nl.rutgerkok.topographica.render.RegionRenderer.DrawnRegion;
import nl.rutgerkok.topographica.scheduler.Computation;
import nl.rutgerkok.topographica.scheduler.ComputationFactory;
import nl.rutgerkok.topographica.scheduler.TGRunnable;
import nl.rutgerkok.topographica.scheduler.TGRunnable.Type;
import nl.rutgerkok.topographica.util.LongArrayList;
import nl.rutgerkok.topographica.util.LongArrayList.LongQueue;
import nl.rutgerkok.topographica.webserver.IntPair;

public class WorldRenderer extends ComputationFactory<LongQueue, DrawnRegion> {

    private static Path getRegionFolder(World world) {
        return world.getWorldFolder().toPath().resolve("region");
    }

    private final List<Canvas> unusedCanvases = new CopyOnWriteArrayList<>();
    private final World world;
    private final WorldConfig worldConfig;
    private final Path imageFolder;

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
        Path file = this.imageFolder.resolve("r." + image.regionX + "." + image.regionZ + ".jpg");

        image.canvas.outputAndReset(file);
        unusedCanvases.add(image.canvas);
    }

    @Override
    public Computation<LongQueue> initialCalculations() {
        return new Computation<>(new TGRunnable<LongQueue>(Type.LONG_RUNNING, "Region finder") {

            @Override
            public void run() throws Throwable {
                LongArrayList regions = new LongArrayList();

                // Add all regions to list
                Pattern regionPattern = Pattern.compile("^r\\.([-0-9]+)\\.([-0-9]+)\\.mca$");
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(getRegionFolder(world))) {
                    for (Path path : stream) {
                        String fileName = path.getFileName().toString();
                        Matcher matcher = regionPattern.matcher(fileName);
                        if (!matcher.matches()) {
                            continue;
                        }
                        int regionX = Integer.parseInt(matcher.group(1));
                        int regionZ = Integer.parseInt(matcher.group(2));
                        if (worldConfig.shouldRender(regionX, regionZ)) {
                            regions.add(IntPair.toLong(regionX, regionZ));
                        }

                        // Check for cancels
                        if (future.isDone()) {
                            return;
                        }
                    }
                }

                // Done
                future.set(regions.toQueue());
            }
        });
    }

    @Override
    public Computation<DrawnRegion> next(LongQueue regionQueue) throws NoSuchElementException {
        long regionLocation = regionQueue.getNext();

        // Create task for next region
        int regionX = IntPair.getX(regionLocation);
        int regionZ = IntPair.getZ(regionLocation);

        Canvas image = grabImage();
        RegionRenderer renderer = new RegionRenderer(worldConfig, world, regionX, regionZ);
        return renderer.getRenderTasks(image);
    }

}
