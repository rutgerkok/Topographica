package nl.rutgerkok.topographica.render;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;

import nl.rutgerkok.topographica.config.ColorConfig;

public final class ChunkRendererModern extends ChunkRenderer {

    public ChunkRendererModern(ColorConfig colorMap) {
        super(colorMap);
    }

    @Override
    protected Material getMaterial(ChunkSnapshot chunk, int x, int y, int z) {
        return chunk.getBlockType(x, y, z);
    }

    @Override
    protected boolean isLiquid(Material material) {
        return material == Material.WATER || material == Material.FLOWING_WATER;
    }
}
