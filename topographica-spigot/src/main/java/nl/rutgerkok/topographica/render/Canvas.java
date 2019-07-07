package nl.rutgerkok.topographica.render;

import static nl.rutgerkok.topographica.util.SizeConstants.PIXEL_SIZE_BLOCKS_BITS;
import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_BLOCKS;
import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_PIXELS;
import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_PIXELS_BITS;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.bukkit.Color;

public final class Canvas {

    /**
     * Creates a drawing canvas from a region from an existing file. Returns a new
     * canvas if the file is not readable.
     *
     * @param imageFile
     *            The image file.
     * @return The canvas.
     */
    public static Canvas createFromFile(Path imageFile) {
        try {
            return new Canvas(imageFile);
        } catch (IOException e) {
            return new Canvas();
        }
    }

    /**
     * Creates a new drawing canvas for a region.
     *
     * @return The drawing canvas.
     */
    public static Canvas createNew() {
        return new Canvas();
    }

    int REGION_SIZE_BLOCKS_MASK = REGION_SIZE_BLOCKS - 1;

    private final BufferedImage image;
    private final int[] pixels;

    private Canvas() {
        this.image = new BufferedImage(REGION_SIZE_PIXELS, REGION_SIZE_PIXELS, BufferedImage.TYPE_INT_RGB);
        this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        clearImage();
    }

    private Canvas(Path imageFile) throws IOException {
        BufferedImage loaded = ImageIO.read(imageFile.toFile());
        if (loaded.getType() != BufferedImage.TYPE_INT_RGB) {
            // Convert the image
            this.image = new BufferedImage(REGION_SIZE_PIXELS, REGION_SIZE_PIXELS, BufferedImage.TYPE_INT_RGB);
            this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            loaded.getRGB(0, 0, REGION_SIZE_PIXELS, REGION_SIZE_PIXELS, pixels, 0, REGION_SIZE_PIXELS);
        } else {
            this.image = loaded;
            this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        }

        if (this.pixels.length != REGION_SIZE_PIXELS * REGION_SIZE_PIXELS) {
            throw new IOException("Found image of wrong size");
        }

    }

    public void clearImage() {
        Arrays.fill(pixels, Color.BLACK.asRGB());
    }

    public Graphics2D createGraphics() {
        return image.createGraphics();
    }

    /**
     * Used by the painter to add a pixel.
     *
     * @param blockXInWorld
     *            The block x.
     * @param blockZInWorld
     *            The block z.
     * @param color
     *            The color.
     */
    public void setColor(int blockXInWorld, int blockZInWorld, Color color) {
        int x = (blockXInWorld & REGION_SIZE_BLOCKS_MASK) >> PIXEL_SIZE_BLOCKS_BITS;
        int z = (blockZInWorld & REGION_SIZE_BLOCKS_MASK) >> PIXEL_SIZE_BLOCKS_BITS;
        pixels[x | z << REGION_SIZE_PIXELS_BITS] = color.asRGB();
    }

    public void writeToFile(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "PNG", file.toFile());
    }

}
