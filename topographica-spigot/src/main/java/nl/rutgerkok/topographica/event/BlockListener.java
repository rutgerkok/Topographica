package nl.rutgerkok.topographica.event;

import java.util.Objects;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import nl.rutgerkok.topographica.render.ServerTaskList;

public final class BlockListener implements Listener {

    private final ServerTaskList renderer;

    public BlockListener(ServerTaskList renderer) {
        this.renderer = Objects.requireNonNull(renderer);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        this.renderer.askToRenderBlock(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        this.renderer.askToRenderBlock(block);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) {
            this.renderer.askToRenderChunk(event.getChunk());
        }
    }
}
