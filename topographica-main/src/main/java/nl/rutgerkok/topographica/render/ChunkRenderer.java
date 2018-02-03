package nl.rutgerkok.topographica.render;

import static nl.rutgerkok.topographica.util.SizeConstants.CHUNK_SIZE_BLOCKS;
import static nl.rutgerkok.topographica.util.SizeConstants.CHUNK_SIZE_BLOCKS_BITS;
import static nl.rutgerkok.topographica.util.SizeConstants.PIXEL_SIZE_BLOCKS;

import java.util.Objects;

import nl.rutgerkok.topographica.config.ColorConfig;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Color;
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

    protected Color adjustColor(Color color, int y) {
        if (y > 74) {
            // Make lighter
            if (y > 194) {
                y = 194;
            }
            double adjustment = (y - 74) / 200;
            return makeLighter(color, adjustment);
        }
        return color;
    }

    /**
     * Makes the color lighter.
     *
     * @param color
     *            The color to chane.
     * @param amount
     *            Amount: 0 is no change, 1 is completely white.
     * @return
     */
    private Color makeLighter(Color color, double amount) {
        int red = (int) (color.getRed() + (amount * (255 - color.getRed())));
        int green = (int) (color.getGreen() + (amount * (255 - color.getGreen())));
        int blue = (int) (color.getBlue() + (amount * (255 - color.getBlue())));
        return Color.fromRGB(red, green, blue);
    }

    /**
     * Renders the chunk to the canvas.
     *
     * @param chunk
     *            Chunk to render.
     * @param canvas
     *            Canvas to render to.
     */
    @SuppressWarnings("deprecation")
    public void render(ChunkSnapshot chunk, Canvas canvas) {
        for (int x = 0; x < CHUNK_SIZE_BLOCKS; x += PIXEL_SIZE_BLOCKS) {
            for (int z = 0; z < CHUNK_SIZE_BLOCKS; z += PIXEL_SIZE_BLOCKS) {
                int y = chunk.getHighestBlockYAt(x, z);
                int blockId = chunk.getBlockTypeId(x, y, z);
                if (blockId == 0 && y > 0) {
                    // Air, try one block lower
                    blockId = chunk.getBlockTypeId(x, y - 1, z);
                }
                Material material = Material.getMaterial(blockId);

                int worldX = chunk.getX() << CHUNK_SIZE_BLOCKS_BITS | x;
                int worldZ = chunk.getZ() << CHUNK_SIZE_BLOCKS_BITS | z;
                canvas.setColor(worldX, worldZ, adjustColor(colorMap.getColor(material), y));
            }
        }

    }
}
