package nl.rutgerkok.topographica.render;

import org.bukkit.ChunkSnapshot;
import org.bukkit.material.MaterialData;

final class ChunkRenderer {

    private final ColorMap colorMap = new ColorMap();

    void render(ChunkSnapshot chunk, Canvas canvas) {
        for (int x = 0; x < Canvas.CHUNK_SIZE_BLOCKS; x += Canvas.PIXEL_SIZE_BLOCKS) {
            for (int z = 0; z < Canvas.CHUNK_SIZE_BLOCKS; z += Canvas.PIXEL_SIZE_BLOCKS) {
                int y = chunk.getHighestBlockYAt(x, z) - 1;
                @SuppressWarnings("deprecation")
                MaterialData blockData = chunk.getBlockType(x, y, z).getNewData((byte) chunk.getBlockData(x, y, z));
                
                int worldX = chunk.getX() << Canvas.CHUNK_SIZE_BLOCKS_BITS | x;
                int worldZ = chunk.getZ() << Canvas.CHUNK_SIZE_BLOCKS_BITS | z;
                canvas.setColor(worldX, worldZ, colorMap.getColor(blockData));
            }
        }

    }
}
