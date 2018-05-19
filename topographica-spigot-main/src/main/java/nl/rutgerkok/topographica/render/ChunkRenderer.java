package nl.rutgerkok.topographica.render;

import static nl.rutgerkok.topographica.util.SizeConstants.CHUNK_SIZE_BLOCKS;
import static nl.rutgerkok.topographica.util.SizeConstants.CHUNK_SIZE_BLOCKS_BITS;
import static nl.rutgerkok.topographica.util.SizeConstants.PIXEL_SIZE_BLOCKS;

import java.util.Objects;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Color;
import org.bukkit.Material;

import nl.rutgerkok.topographica.config.WorldConfig;

public class ChunkRenderer {

    /**
     * Creates a chunk renderer. Automatically switches to the new one on
     * Minecraft 1.13.
     *
     * @param worldConfig
     *            Configuration file.
     * @return The chunk renderer.
     */
    public static ChunkRenderer create(WorldConfig worldConfig) {
        try {
            // Use old class if ChunkSnapshot.getBlockTypeId exists
            ChunkSnapshot.class.getMethod("getBlockTypeId", int.class, int.class, int.class);
            return new ChunkRenderer(worldConfig);
        } catch (NoSuchMethodException e) {
            // Try the new class
            try {
                Class<?> chunkRendererModern = Class.forName(ChunkRenderer.class + "Modern");
                return (ChunkRenderer) chunkRendererModern.getConstructor(WorldConfig.class).newInstance(worldConfig);
            } catch (ReflectiveOperationException e1) {
                throw new RuntimeException(e1);
            }
        }
    }

    protected final WorldConfig worldConfig;

    protected ChunkRenderer(WorldConfig worldConfig) {
        this.worldConfig = Objects.requireNonNull(worldConfig, "worldConfig");
    }

    protected Color adjustColor(Color color, int y) {
        // Clamp to 10 - 190
        if (y > 190) {
            y = 190;
        } else if (y < 10) {
            y = 10;
        }

        // Rescale from 10 - 190 to 0 - 1
        double adjustment = (y - 10) / 180.0;
        if (adjustment < 0.49) {
            return makeDarker(color, adjustment + 0.5);
        } else if (adjustment > 0.51) {
            return makeLighter(color, adjustment - 0.5);
        }

        return color;
    }

    /**
     * This method moves the y to the bottom of the sea, so that the sea can be
     * made darker.
     *
     * @param chunk
     *            The chunk.
     * @param x
     *            X pos in the chunk.
     * @param y
     *            Y pos in the chunk.
     * @param z
     *            Z pos in the chunk.
     * @return The y pos of the lowest liquid block.
     */
    protected final int fixYForLiquid(ChunkSnapshot chunk, int x, int y, int z) {
        while (y > 0) {
            y--;
            Material material = getMaterial(chunk, x, y, z);
            if (!isLiquid(material)) {
                return y + 1;
            }
        }
        return y;
    }

    @SuppressWarnings("deprecation")
    protected Material getMaterial(ChunkSnapshot chunk, int x, int y, int z) {
        // New methods are not yet available in Minecraft 1.8 - 1.11
        if (x < 0 || x >= CHUNK_SIZE_BLOCKS || z < 0 || z >= CHUNK_SIZE_BLOCKS || y < 0 || y >= 256) {
            throw new IllegalArgumentException("Invalid pos: " + x + "," + y + "," + z);
        }
        return Material.getMaterial(chunk.getBlockTypeId(x, y, z));
    }

    protected boolean isLiquid(Material material) {
        // New material names are not yet available in Minecraft 1.8 - 1.12
        return material == Material.WATER || material == Material.STATIONARY_WATER;
    }

    /**
     * Makes the color darker.
     *
     * @param color
     *            The original color.
     * @param adjustment
     *            Amount: 1 is no change, 0 is completely black.
     * @return The adjusted color.
     */
    private Color makeDarker(Color color, double adjustment) {
        int red = (int) (color.getRed() * adjustment);
        int green = (int) (color.getGreen() * adjustment);
        int blue = (int) (color.getBlue() * adjustment);
        return Color.fromRGB(red, green, blue);
    }

    /**
     * Makes the color lighter.
     *
     * @param color
     *            The original color.
     * @param amount
     *            Amount: 0 is no change, 1 is completely white.
     * @return The adjusted color.
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
    public void render(ChunkSnapshot chunk, Canvas canvas) {
        for (int x = 0; x < CHUNK_SIZE_BLOCKS; x += PIXEL_SIZE_BLOCKS) {
            for (int z = 0; z < CHUNK_SIZE_BLOCKS; z += PIXEL_SIZE_BLOCKS) {
                int worldX = chunk.getX() << CHUNK_SIZE_BLOCKS_BITS | x;
                int worldZ = chunk.getZ() << CHUNK_SIZE_BLOCKS_BITS | z;
                if (!this.worldConfig.getRenderArea().shouldRenderColumn(worldX, worldZ)) {
                    continue;
                }
                int y = chunk.getHighestBlockYAt(x, z);
                Material material = getMaterial(chunk, x, y, z);
                if (material == Material.AIR && y > 0) {
                    // Air, try one block lower
                    y--;
                    material = getMaterial(chunk, x, y, z);
                }
                y = fixYForLiquid(chunk, x, y, z);

                canvas.setColor(worldX, worldZ, adjustColor(worldConfig.getColors().getColor(material), y));
            }
        }

    }
}
