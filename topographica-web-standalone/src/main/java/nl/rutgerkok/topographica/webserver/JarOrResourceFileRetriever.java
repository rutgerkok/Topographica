package nl.rutgerkok.topographica.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class JarOrResourceFileRetriever implements BundledFiles {

    private final Path devDirectory = Paths.get("../topographica-web-shared/src/main/resources");
    private final boolean devDirectoryExists = Files.exists(devDirectory);

    @Override
    public InputStream getResource(String string) {
        if (devDirectoryExists) {
            try {
                return Files.newInputStream(devDirectory.resolve(string));
            } catch (IOException e) {
                return null;
            }
        }

        if (!string.startsWith("/")) {
            string = "/" + string;
        }
        return JarOrResourceFileRetriever.class.getResourceAsStream(string);
    }

}
