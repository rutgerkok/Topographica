package nl.rutgerkok.topographica.render;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.World;

import nl.rutgerkok.topographica.render.WorldTaskList.DrawInstruction;
import nl.rutgerkok.topographica.util.Region;

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
    public void loadFromQueue(Server server, ServerTaskList renderer) {
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
                DrawInstruction drawInstruction = parseLine(line);
                renderer.askToRender(world, drawInstruction);
            }
            Files.deleteIfExists(savedQueueFile);
        } catch (NoSuchFileException e) {
            // Ignore
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load region queue", e);
            createBackup();
        }
    }

    private DrawInstruction parseLine(String line) throws IOException {
        String[] split = line.split(" ");
        boolean isRegion = true;
        if (split.length == 2) {
            line = split[1];
            isRegion = split[0].equals("region");
        }
        int commaIndex = line.indexOf(',');
        if (commaIndex == -1) {
            throw new IOException("No comma on line \"" + line + "\"");
        }
        try {
            int x = Integer.parseInt(line.substring(0, commaIndex));
            int z = Integer.parseInt(line.substring(commaIndex + 1));
            if (isRegion) {
                return DrawInstruction.ofRegion(Region.of(x, z));
            }
            return DrawInstruction.ofChunk(x, z);
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
    public void saveRegionQueue(ServerTaskList renderer) {
        try {
            Files.createDirectories(savedQueueFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(savedQueueFile,
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                for (Entry<UUID, WorldTaskList> entry : renderer.getActiveTaskLists().entrySet()) {
                    WorldTaskList taskList = entry.getValue();
                    writeWorldHeader(writer, entry.getKey(), taskList);

                    for (DrawInstruction region : taskList.getQueueSnapshot()) {
                        if (region.isRegion) {
                            writer.write("region ");
                        } else {
                            writer.write("chunk ");
                        }
                        writer.write(String.valueOf(region.x));
                        writer.write(',');
                        writer.write(String.valueOf(region.z));
                        writer.newLine();
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write region queue", e);
        }
    }

    private void writeWorldHeader(BufferedWriter writer, UUID world, WorldTaskList worldRenderer) throws IOException {
        writer.write(WORLD_PREFIX);
        writer.write(world.toString());
        writer.newLine();
    }

}
