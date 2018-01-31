package nl.rutgerkok.topographica.render;

import static nl.rutgerkok.topographica.util.SizeConstants.PIXEL_SIZE_BLOCKS_BITS;
import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_BLOCKS;
import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_PIXELS;
import static nl.rutgerkok.topographica.util.SizeConstants.REGION_SIZE_PIXELS_BITS;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.bukkit.Color;

/**
 * Raw data of an image. Displays 1024x1024 blocks.
 */
public class RawImage implements Canvas {

    private static final int REGION_SIZE_BLOCKS_MASK = REGION_SIZE_BLOCKS - 1;

    private final BufferedImage image;
    private final int[] pixels;

    public RawImage() {
        this.image = new BufferedImage(REGION_SIZE_PIXELS, REGION_SIZE_PIXELS, BufferedImage.TYPE_INT_RGB);
        this.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        clearImage();
    }

    private void clearImage() {
        Arrays.fill(pixels, Color.BLACK.asRGB());
    }

    public void outputAndReset(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "JPG", file.toFile());
    }

    @Override
    public void setColor(int blockXInWorld, int blockZInWorld, Color color) {
        int x = (blockXInWorld & REGION_SIZE_BLOCKS_MASK) >> PIXEL_SIZE_BLOCKS_BITS;
        int z = (blockZInWorld & REGION_SIZE_BLOCKS_MASK) >> PIXEL_SIZE_BLOCKS_BITS;
        pixels[x | z << REGION_SIZE_PIXELS_BITS] = color.asRGB();
    }
}
