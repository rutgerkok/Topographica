package nl.rutgerkok.topographica.render;

import static nl.rutgerkok.topographica.util.SizeConstants.CHUNK_SIZE_BLOCKS;
import static nl.rutgerkok.topographica.util.SizeConstants.CHUNK_SIZE_BLOCKS_BITS;
import static nl.rutgerkok.topographica.util.SizeConstants.PIXEL_SIZE_BLOCKS;

import org.bukkit.ChunkSnapshot;
import org.bukkit.material.MaterialData;

final class ChunkRenderer {

    private final ColorMap colorMap = new ColorMap();

    void render(ChunkSnapshot chunk, Canvas canvas) {
        for (int x = 0; x < CHUNK_SIZE_BLOCKS; x += PIXEL_SIZE_BLOCKS) {
            for (int z = 0; z < CHUNK_SIZE_BLOCKS; z += PIXEL_SIZE_BLOCKS) {
                int y = chunk.getHighestBlockYAt(x, z) - 1;
                @SuppressWarnings("deprecation")
                MaterialData blockData = chunk.getBlockType(x, y, z).getNewData((byte) chunk.getBlockData(x, y, z));
                
                int worldX = chunk.getX() << CHUNK_SIZE_BLOCKS_BITS | x;
                int worldZ = chunk.getZ() << CHUNK_SIZE_BLOCKS_BITS | z;
                canvas.setColor(worldX, worldZ, colorMap.getColor(blockData));
            }
        }

    }
}
