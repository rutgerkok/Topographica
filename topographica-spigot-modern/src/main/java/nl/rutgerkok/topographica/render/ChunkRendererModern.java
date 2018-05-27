package nl.rutgerkok.topographica.render;

import nl.rutgerkok.topographica.config.WorldConfig;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;

public final class ChunkRendererModern extends ChunkRenderer {

    public ChunkRendererModern(WorldConfig worldConfig) {
        super(worldConfig);
    }

    @Override
    protected Material getMaterial(ChunkSnapshot chunk, int x, int y, int z) {
        return chunk.getBlockType(x, y, z);
    }

    @Override
    protected boolean isLiquid(Material material) {
        return material == Material.WATER;
    }
}
