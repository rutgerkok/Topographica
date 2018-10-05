package nl.rutgerkok.topographica.render;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import nl.rutgerkok.topographica.util.Region;

import org.bukkit.Server;
import org.bukkit.World;

/**
 * Class for saving and loading the rendering queue to/from disk.
 *
 */
public final class ChunkQueuePersistance {

    private static final String WORLD_PREFIX = "world_id=";

    private final Path savedQueueFile;
    private final Logger logger;

    public ChunkQueuePersistance(Path savedQueueFile, Logger logger) {
        this.savedQueueFile = Objects.requireNonNull(savedQueueFile, "savedQueueFile");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    private void createBackup() {
        String fileName = savedQueueFile.getFileName().toString();
        Path forensicCopy = savedQueueFile.resolveSibling(fileName + "-ERRORED.txt");
        try {
            Files.copy(savedQueueFile, forensicCopy);
        } catch (IOException e2) {
            // Ignore
        }
    }

    /**
     * Adds the regions stored in the queue file to the given renderer.
     *
     * @param server
     *            The server, for looking up worlds.
     * @param renderer
     *            The renderer.
     */
    public void loadFromQueue(Server server, ServerRenderer renderer) {
        try (BufferedReader reader = Files.newBufferedReader(savedQueueFile, StandardCharsets.UTF_8)) {
            String line;
            World world = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(WORLD_PREFIX)) {
                    UUID worldId = UUID.fromString(line.substring(WORLD_PREFIX.length()));
                    world = server.getWorld(worldId);
                    if (world == null) {
                        throw new IOException("World with id " + worldId + " is gone. Did you delete a world? If yes,"
                                + " you can ignore this error.");
                    }
                    continue;
                }
                if (line.startsWith("#") || line.isEmpty()) {
                    continue; // Comment
                }

                if (world == null) {
                    throw new IOException("No world declared for line \"" + line + "\"");
                }
                Region region = parseLine(line);
                renderer.askToRenderRegion(world, region);
            }
            Files.deleteIfExists(savedQueueFile);
        } catch (NoSuchFileException e) {
            // Ignore
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load region queue", e);
            createBackup();
        }
    }

    private Region parseLine(String line) throws IOException {
        int commaIndex = line.indexOf(',');
        if (commaIndex == -1) {
            throw new IOException("No comma on line \"" + line + "\"");
        }
        try {
            int regionX = Integer.parseInt(line.substring(0, commaIndex));
            int regionZ = Integer.parseInt(line.substring(commaIndex + 1));
            return Region.of(regionX, regionZ);
        } catch (NumberFormatException e) {
            throw new IOException("Cannot read two number on line \"" + line + "\"");
        }

    }

    /**
     * Saves all regions in the rendering queue to a file.
     *
     * @param renderer
     *            The renderer.
     */
    public void saveRegionQueue(ServerRenderer renderer) {
        try {
            Files.createDirectories(savedQueueFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(savedQueueFile,
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                for (WorldRenderer worldRenderer : renderer.getActiveRenderers()) {
                    writeWorldHeader(writer, worldRenderer);

                    for (Region region : worldRenderer.getQueueSnapshot()) {
                        writer.write(String.valueOf(region.getRegionX()));
                        writer.write(',');
                        writer.write(String.valueOf(region.getRegionZ()));
                        writer.newLine();
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write region queue", e);
        }
    }

    private void writeWorldHeader(BufferedWriter writer, WorldRenderer worldRenderer) throws IOException {
        World world = worldRenderer.getWorld();
        writer.write(WORLD_PREFIX);
        writer.write(world.getUID().toString());
        writer.newLine();
        writer.write("# The last-known name of this world was \"");
        writer.write(world.getName());
        writer.write('"');
        writer.newLine();
    }

}
