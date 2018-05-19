package nl.rutgerkok.topographica.webserver;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableList;

final class LastKnownServerInfo extends ServerInfo {

    private static class CachedWorld implements WebWorld {
        private final String name;

        CachedWorld(String name) {
            this.name = Objects.requireNonNull(name, "name");
        }

        @Override
        public String getDisplayName() {
            // Display names not yet supported
            return name;
        }

        @Override
        public String getFolderName() {
            return name;
        }

        @Override
        public int[] getOrigin() {
            // Origin not yet supported
            return new int[] { 0, 0, 0 };
        }
    }

    private static final Logger logger = ServerLogger.setup(LastKnownServerInfo.class);
    private final Path imagesFolder;
    private final int port;
    private final List<CachedWorld> worlds;

    LastKnownServerInfo(Path imagesFolder, int port) {
        this.imagesFolder = Objects.requireNonNull(imagesFolder, "imagesFolder");
        this.port = port;

        // Find worlds using directories
        ImmutableList.Builder<CachedWorld> worlds = ImmutableList.builder();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(imagesFolder)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    worlds.add(new CachedWorld(path.getFileName().toString()));
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error reading world list", e);
        }
        this.worlds = worlds.build();
    }

    @Override
    public Path getImagesFolder() {
        return imagesFolder;
    }

    @Override
    public Collection<? extends WebPlayer> getPlayers(WebWorld world) {
        // Player updates not yet implemented for standalone servers
        return Collections.emptyList();
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public Collection<? extends WebWorld> getWorlds() {
        return worlds;
    }

}
