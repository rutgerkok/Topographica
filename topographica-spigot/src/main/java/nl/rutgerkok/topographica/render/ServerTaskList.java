package nl.rutgerkok.topographica.render;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.ImmutableMap;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;

import nl.rutgerkok.topographica.config.Config;
import nl.rutgerkok.topographica.config.WorldConfig;
import nl.rutgerkok.topographica.render.WorldTaskList.DrawInstruction;
import nl.rutgerkok.topographica.util.Region;

/**
 * The class that is responsible for rendering all worlds on the server.
 *
 */
public class ServerTaskList {

    private final ConcurrentMap<UUID, WorldTaskList> taskLists = new ConcurrentHashMap<>();
    private final Config config;

    public ServerTaskList(Config config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * General method to (re-)render something.
     *
     * @param world
     *            The world.
     * @param drawInstruction
     *            The thing to re-render.
     */
    public void askToRender(World world, DrawInstruction drawInstruction) {
        WorldTaskList renderer = getTaskList(world);
        renderer.requestRedraw(drawInstruction);
    }

    /**
     * Puts the region of a block in the queue for rendering.
     *
     * @param block
     *            The block.
     */
    public void askToRenderBlock(Block block) {
        askToRender(block.getWorld(), DrawInstruction.ofChunk(block.getX() >> 4, block.getZ() >> 4));
    }

    /**
     * Puts the chunk in the queue for rendering.
     * 
     * @param chunk
     *            The chunk.
     */
    public void askToRenderChunk(Chunk chunk) {
        this.askToRender(chunk.getWorld(), DrawInstruction.ofChunk(chunk.getX(), chunk.getZ()));
    }

    /**
     * Puts a single region in the queue for rendering.
     *
     * @param world
     *            The world.
     * @param region
     *            The region.
     */
    public void askToRenderRegion(World world, Region region) {
        askToRender(world, DrawInstruction.ofRegion(region));
    }

    /**
     * Gets an immutable list of all registered renderers.
     *
     * @return The renderers.
     */
    public Map<UUID, WorldTaskList> getActiveTaskLists() {
        return ImmutableMap.copyOf(taskLists);
    }

    private WorldTaskList getTaskList(World world) {
        UUID uuid = world.getUID();
        WorldTaskList renderer = taskLists.get(uuid); // A
        if (renderer == null) {
            // Ok, create one
            WorldConfig worldConfig = config.getWorldConfig(world);
            renderer = new WorldTaskList(worldConfig.getRenderArea());
            WorldTaskList justAdded = taskLists.putIfAbsent(uuid, renderer); // B
            if (justAdded != null) {
                // Interesting, another thread just created a renderer between A
                // and B. Use that one instead
                renderer = justAdded;
            }
        }
        return renderer;
    }

}
