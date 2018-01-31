package nl.rutgerkok.topographica.render;

import static nl.rutgerkok.topographica.util.SizeConstants.CHUNK_SIZE_BLOCKS;
import static nl.rutgerkok.topographica.util.SizeConstants.CHUNK_SIZE_BLOCKS_BITS;
import static nl.rutgerkok.topographica.util.SizeConstants.PIXEL_SIZE_BLOCKS;

import java.util.Objects;

import nl.rutgerkok.topographica.config.ColorConfig;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;

public class ChunkRenderer {

    /**
     * Creates a chunk renderer. Automatically switches to the new one on
     * Minecraft 1.13.
     *
     * @param colorMap
     *            Colors of the materials.
     * @return The chunk renderer.
     */
    public static ChunkRenderer create(ColorConfig colorMap) {
        try {
            // Use old class if ChunkSnapshot.getBlockTypeId exists
            ChunkSnapshot.class.getMethod("getBlockTypeId", int.class, int.class, int.class);
            return new ChunkRenderer(colorMap);
        } catch (NoSuchMethodException e) {
            // Try the new class
            try {
                Class<?> chunkRendererModern = Class.forName(ChunkRenderer.class + "Modern");
                return (ChunkRenderer) chunkRendererModern.getConstructor(ColorConfig.class).newInstance(colorMap);
            } catch (ReflectiveOperationException e1) {
                throw new RuntimeException(e1);
            }
        }
    }

    protected final ColorConfig colorMap;

    protected ChunkRenderer(ColorConfig colorMap) {
        this.colorMap = Objects.requireNonNull(colorMap, "colorMap");
    }

    /**
     * Renders the chunk to the canvas.
     *
     * @param chunk
     *            Chunk to render.
     * @param canvas
     *            Canvas to render to.
     */
    public void render(ChunkSnapshot chunk, Canvas canvas) {
        for (int x = 0; x < CHUNK_SIZE_BLOCKS; x += PIXEL_SIZE_BLOCKS) {
            for (int z = 0; z < CHUNK_SIZE_BLOCKS; z += PIXEL_SIZE_BLOCKS) {
                int y = chunk.getHighestBlockYAt(x, z) - 1;
                @SuppressWarnings("deprecation")
                Material material = Material.getMaterial(chunk.getBlockTypeId(x, y, z));

                int worldX = chunk.getX() << CHUNK_SIZE_BLOCKS_BITS | x;
                int worldZ = chunk.getZ() << CHUNK_SIZE_BLOCKS_BITS | z;
                canvas.setColor(worldX, worldZ, colorMap.getColor(material));
            }
        }

    }
}
