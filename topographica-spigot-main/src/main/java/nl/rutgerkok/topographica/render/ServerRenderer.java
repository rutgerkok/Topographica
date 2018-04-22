package nl.rutgerkok.topographica.render;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import nl.rutgerkok.topographica.config.Config;
import nl.rutgerkok.topographica.scheduler.Scheduler;
import nl.rutgerkok.topographica.scheduler.TGRunnable;
import nl.rutgerkok.topographica.scheduler.TGRunnable.Type;
import nl.rutgerkok.topographica.util.Region;

import com.google.common.collect.ImmutableList;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 * The class that is responsible for rendering all worlds on the server.
 *
 */
public class ServerRenderer implements Listener {

    private final Scheduler scheduler;
    private final Config config;
    private final ConcurrentMap<UUID, WorldRenderer> activeRenderers = new ConcurrentHashMap<>();

    public ServerRenderer(Scheduler scheduler, Config config) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Gets an immutable list of all registered renderers.
     *
     * @return The renderers.
     */
    public List<WorldRenderer> getActiveRenderers() {
        return ImmutableList.copyOf(activeRenderers.values());
    }

    private WorldRenderer getRenderer(World world) {
        UUID uuid = world.getUID();
        WorldRenderer renderer = activeRenderers.get(uuid); // A
        if (renderer == null) {
            // Ok, create one
            renderer = new WorldRenderer(world, config);
            WorldRenderer justAdded = activeRenderers.putIfAbsent(uuid, renderer); // B
            if (justAdded != null) {
                // Interesting, another thread just created a renderer between A
                // and B. Use that one instead
                renderer = justAdded;
            }
        }
        return renderer;
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        World world = event.getWorld();
        WorldRenderer renderer = activeRenderers.remove(world.getUID());
        // From this point on, if the world gets reloaded, regions get submitted
        // to a new renderer
        if (renderer != null) {
            renderer.clearRegionQueue();
        }
    }

    /**
     * Puts all regions of a world in a queue for rendering.
     *
     * @param world
     *            The world.
     */
    public void renderAllRegionsAsync(final World world) {
        scheduler.submit(new TGRunnable<Void>(Type.LONG_RUNNING, "worldRegionFinder") {

            @Override
            public void run() throws IOException {
                WorldRenderer renderer = getRenderer(world);
                renderer.addAllRegions();

                // Reactivate renderer if necessary
                scheduler.submitFactory(renderer);
            }
        });
    }

    /**
     * Puts a single region in the queue for rendering.
     *
     * @param world
     *            The world.
     * @param region
     *            The region.
     */
    public void renderRegion(World world, Region region) {
        WorldRenderer renderer = getRenderer(world);
        renderer.addRegion(region);

        // Reactivate renderer if necessary
        scheduler.submitFactory(renderer);
    }
}
