package nl.rutgerkok.topographica.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.bukkit.World;

/**
 * Used to find the available regions of a world.
 *
 */
public class RegionFilesFinder {

    private static Path getRegionFolder(World world) throws IOException {
        Path worldFolder = world.getWorldFolder().toPath();
        switch (world.getEnvironment()) {
            case NETHER:
                return worldFolder.resolve("DIM-1").resolve("region");
            case NORMAL:
                return worldFolder.resolve("region");
            case THE_END:
                return worldFolder.resolve("DIM1").resolve("region");
            default:
                throw new IOException("Unknown dimension: " + world.getEnvironment());
        }
    }

    /**
     * Gets all existing region files of the world. Should be called
     * asynchronously, as it'll take some time to execute this.
     *
     * @param world
     *            The world.
     * @return The region files.
     * @throws IOException
     *             If reading the world files fails.
     */
    public static Stream<Region> getRegions(World world) throws IOException {
        Path regionFolder = getRegionFolder(world);
        return Files.list(regionFolder).flatMap(RegionFilesFinder::pathToRegion);
    }

    private static Stream<Region> pathToRegion(Path regionFile) {
        String[] fileParts = regionFile.getFileName().toString().split("\\.");
        if (fileParts.length != 4) {
            return Stream.empty();
        }
        if (!fileParts[3].equals("mca") || !fileParts[0].equals("r")) {
            return Stream.empty();
        }
        try {
            int regionX = Integer.parseInt(fileParts[1]);
            int regionZ = Integer.parseInt(fileParts[2]);
            return Stream.of(Region.of(regionX, regionZ));
        } catch (NumberFormatException e) {
            return Stream.empty();
        }
    }

    private RegionFilesFinder() {
        // No instances
    }
}
