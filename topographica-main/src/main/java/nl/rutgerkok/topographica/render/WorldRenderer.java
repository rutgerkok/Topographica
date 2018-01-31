package nl.rutgerkok.topographica.render;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import nl.rutgerkok.topographica.config.Config;
import nl.rutgerkok.topographica.config.WorldConfig;
import nl.rutgerkok.topographica.util.LongArrayList;

public class WorldRenderer {

    /**
     * Renders the whole world.
     *
     * @param plugin
     *            The plugin, for scheduling purposes.
     * @param world
     *            The world.
     * @param config
     *            The configuration file.
     * @return A future, for reaction when the process has completed.
     */
    public static ListenableFuture<World> renderWorld(Plugin plugin, World world, final Config config) {
        final WorldRenderer renderer = new WorldRenderer(plugin, world, config);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

            @Override
            public void run() {
                renderer.start(config);
            }
        });
        return renderer.whenDone;
    }

    private final LongArrayList regions = new LongArrayList();
    private final AtomicInteger nextRegionIndex = new AtomicInteger(0);
    private final List<RawImage> unusedCanvases = new CopyOnWriteArrayList<>();
    private final World world;
    private final WorldConfig worldConfig;
    private final Plugin plugin;
    private final Executor asyncExecutor = new Executor() {

        @Override
        public void execute(Runnable command) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, command);
        }
    };
    private final Runnable renderNext = new Runnable() {

        @Override
        public void run() {
            renderNext();
        }
    };

    private final SettableFuture<World> whenDone = SettableFuture.create();

    /**
     * Creates a renderer for the full world.
     *
     * @param plugin
     *            The plugin.
     * @param world
     *            The world to render.
     * @param config
     *            Plugin configuration.
     * @throws IOException
     *             If an error occurs reading the region directory.
     */
    public WorldRenderer(Plugin plugin, World world, Config config) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.world = Objects.requireNonNull(world, "world");
        this.worldConfig = config.getConfig(world);
    }

    private Path getRegionFolder(World world) {
        return world.getWorldFolder().toPath().resolve("region");
    }

    /**
     * Grabs an unused image, or creates a new one if none available.
     *
     * @return The image.
     */
    private RawImage grabImage() {
        try {
            return unusedCanvases.remove(0);
        } catch (IndexOutOfBoundsException e) {
            // No unused images - create new one
            return new RawImage();
        }
    }

    /**
     * Renders the next region.
     */
    private void renderNext() {
        if (whenDone.isCancelled()) {
            return;
        }

        int nextRegionIndex = this.nextRegionIndex.getAndIncrement();
        if (nextRegionIndex >= regions.size()) {
            // Done!
            whenDone.set(world);
            return;
        }

        long regionLocation = regions.get(nextRegionIndex);
        final int regionX = (int) (regionLocation >>> Integer.SIZE);
        final int regionZ = (int) regionLocation;

        final RawImage image = grabImage();
        RegionRenderer regionRenderer = new RegionRenderer(worldConfig, world, regionX, regionZ);
        plugin.getLogger().info("Starting render of region " + regionX + ", " + regionZ);
        regionRenderer.render(plugin, image).addListener(new Runnable() {

            @Override
            public void run() {
                Path file = plugin.getDataFolder().toPath()
                        .resolve("images")
                        .resolve(world.getName())
                        .resolve("r." + regionX + "." + regionZ + ".jpg");
                try {
                    image.outputAndReset(file);
                    unusedCanvases.add(image);
                    plugin.getLogger().info("Done rendering region " + regionX + ", " + regionZ);
                    renderNext();
                } catch (IOException e) {
                    whenDone.setException(e);
                }

            }
        }, asyncExecutor);
    }

    private void start(Config config) {
        // Add all regions to list
        Pattern regionPattern = Pattern.compile("^r\\.([-0-9]+)\\.([-0-9]+)\\.mca$");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(getRegionFolder(world))) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                Matcher matcher = regionPattern.matcher(fileName);
                if (!matcher.matches()) {
                    continue;
                }
                long regionX = Integer.parseInt(matcher.group(1)) & 0x00000000ffffffffL;
                long regionZ = Integer.parseInt(matcher.group(2)) & 0x00000000ffffffffL;
                regions.add((regionX << Integer.SIZE) | regionZ);
            }
        } catch (IOException e) {
            whenDone.setException(e);
            return;
        }

        if (whenDone.isCancelled()) {
            return;
        }

        // Run as many region drawers as requested
        plugin.getLogger().info("Rendering " + regions.size() + " regions...");
        int workers = config.getWorkers();
        for (int i = 0; i < workers; i++) {
            asyncExecutor.execute(renderNext);
        }
    }

}
