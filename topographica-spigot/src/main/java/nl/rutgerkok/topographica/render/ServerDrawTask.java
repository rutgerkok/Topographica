package nl.rutgerkok.topographica.render;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Server;
import org.bukkit.World;

import nl.rutgerkok.topographica.config.Config;
import nl.rutgerkok.topographica.config.RenderAreaConfig;
import nl.rutgerkok.topographica.config.WorldConfig;
import nl.rutgerkok.topographica.render.WorldTaskList.DrawContext;
import nl.rutgerkok.topographica.util.Coordinate;
import nl.rutgerkok.topographica.util.ServerThreadGetter;

/**
 * A task that runs forever and draws everything.
 *
 */
public class ServerDrawTask implements Runnable {
    private static class RenderingDrawContext implements DrawContext {

        private volatile boolean mustStop = false;
        private final ChunkGetter chunkGetter;
        private final Path mapFolder;
        private final ChunkRenderer renderer;
        private final RenderAreaConfig renderArea;

        public RenderingDrawContext(ChunkGetter chunkGetter, Path mapFolder, ChunkRenderer renderer,
                RenderAreaConfig renderArea) {
            this.chunkGetter = Objects.requireNonNull(chunkGetter, "chunkGetter");
            this.mapFolder = Objects.requireNonNull(mapFolder, "mapFolder");
            this.renderer = Objects.requireNonNull(renderer, "renderer");
            this.renderArea = Objects.requireNonNull(renderArea, "renderArea");
        }

        @Override
        public void drawChunk(Canvas canvas, int chunkX, int chunkZ) {
            if (!renderArea.shouldRenderChunk(chunkX, chunkZ)) {
                return;
            }
            chunkGetter.getChunk(chunkX, chunkZ).ifPresent(chunk -> renderer.render(chunk, canvas));
        }

        @Override
        public Path getSaveFile(Coordinate scaledCoords, int zoomLevel) {
            return mapFolder.resolve("zoom-" + zoomLevel)
                    .resolve("r." + scaledCoords.x + "." + scaledCoords.z + ".png");
        }

        @Override
        public boolean mustStop() {
            return mustStop;
        }

        /**
         * Requests a stop of this renderer.
         */
        public void requestStop() {
            this.mustStop = true;
        }

    }

    private class SimpleChunkGetter implements ChunkGetter {

        private final World world;

        SimpleChunkGetter(World world) {
            this.world = Objects.requireNonNull(world, "world");
        }

        @Override
        public Optional<ChunkSnapshot> getChunk(int chunkX, int chunkZ) {
            Future<Optional<ChunkSnapshot>> future = serverThreadGetter.runOnServerThread(() -> {
                boolean alreadyLoaded = world.isChunkLoaded(chunkX, chunkZ);

                if (world.loadChunk(chunkX, chunkZ, false)) {
                    Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                    ChunkSnapshot snapshot = chunk.getChunkSnapshot(true, false, false);
                    if (!alreadyLoaded) {
                        world.unloadChunkRequest(chunkX, chunkZ);
                    }
                    return Optional.of(snapshot);
                }
                return Optional.empty();

            });

            try {
                return future.get();
            } catch (InterruptedException e) {
                return Optional.empty();
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    private final ServerTaskList serverTaskList;
    private final Config config;
    private final Server server;
    private final ServerThreadGetter<Optional<ChunkSnapshot>> serverThreadGetter;

    /**
     * May be null.
     */
    private volatile RenderingDrawContext currentContext;
    private volatile boolean mustStop = false;

    public ServerDrawTask(ServerTaskList serverTaskList, Server server,
            ServerThreadGetter<Optional<ChunkSnapshot>> serverThreadGetter, Config config) {
        this.serverTaskList = Objects.requireNonNull(serverTaskList, "serverTaskList");
        this.server = Objects.requireNonNull(server, "server");
        this.serverThreadGetter = Objects.requireNonNull(serverThreadGetter, "serverThreadGetter");
        this.config = Objects.requireNonNull(config, "config");
    }

    public void requestStop() {
        this.mustStop = true;
        RenderingDrawContext context = this.currentContext;
        if (context != null) {
            context.requestStop();
        }
    }

    @Override
    public void run() {
        while (!mustStop) {
            for (Iterator<Entry<UUID, WorldTaskList>> it = serverTaskList.getActiveTaskLists().entrySet().iterator(); it
                    .hasNext();) {
                Entry<UUID, WorldTaskList> entry = it.next();
                UUID worldId = entry.getKey();
                WorldTaskList worldTaskList = entry.getValue();
                World world = server.getWorld(worldId);
                if (world == null) {
                    // World is unloaded
                    it.remove();
                    continue;
                }

                WorldConfig worldConfig = config.getWorldConfig(world);
                ChunkGetter chunkGetter = new SimpleChunkGetter(world);
                ChunkRenderer chunkRenderer = new ChunkRenderer(worldConfig);
                Path folder = config.getWebConfig().getImagesFolder().resolve(world.getName());
                RenderingDrawContext context = new RenderingDrawContext(chunkGetter, folder, chunkRenderer,
                        worldConfig.getRenderArea());

                currentContext = context;
                if (mustStop) {
                    // If requestStop() was called just before
                    // this.currentContext was set, then
                    // this.currentContext would never be notified of the stop
                    // request
                    break;
                }
                try {
                    worldTaskList.drawAll(context);
                } catch (IOException e) {
                    throw new RuntimeException("Map renderer crashed", e);
                }

                try {
                    // Sleep between world renders, so that our infinite loop
                    // doesn't take forever
                    // to execute
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    if (mustStop) {
                        break;
                    }
                }
            }
        }
        currentContext = null;
    }

}
