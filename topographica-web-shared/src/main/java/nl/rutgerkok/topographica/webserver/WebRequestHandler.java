package nl.rutgerkok.topographica.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

final class WebRequestHandler {

    private static final String IMAGES_URL = "/" + WebPaths.IMAGES + "/";

    private final BundledFiles bundledFiles;
    private final Logger logger;
    private final WebConfigInterface webConfig;

    WebRequestHandler(BundledFiles files, WebConfigInterface webConfig, Logger logger) {
        this.bundledFiles = Objects.requireNonNull(files, "files");
        this.webConfig = Objects.requireNonNull(webConfig, "webConfig");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    private String getMime(String fileName) {
        if (fileName.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (fileName.endsWith(".css")) {
            return "text/css";
        }
        throw new UnsupportedOperationException("Unkown MIME: " + fileName);
    }

    public FullHttpResponse respond(FullHttpRequest request) throws IOException {
        try {
            String uri = request.uri();

            // Images must be sent from a folder, not from the JAR file
            if (uri.startsWith(IMAGES_URL)) {
                return sendImage(uri.substring(IMAGES_URL.length()));
            }

            return sendFromJarFile(toFile(uri), HttpResponseStatus.OK);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling web request to " + request.uri(), e);
            return send500();
        }
    }

    private FullHttpResponse send404() throws IOException {
        return sendFromJarFile("404.html", HttpResponseStatus.NOT_FOUND);
    }

    private FullHttpResponse send500() throws IOException {
        return sendFromJarFile("500.html", HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    private FullHttpResponse sendFromJarFile(String file, HttpResponseStatus code) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        try (InputStream stream = bundledFiles.getResource("web/" + file)) {
            if (stream == null) {
                if (file.equals("404.html")) {
                    // Protect against infinite loop
                    throw new Error("404 page is missing");
                }
                return send404();
            }
            buffer.writeBytes(stream, 20 * 1024);
        }

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, code, buffer);

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, getMime(file));
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buffer.writerIndex());

        return response;
    }

    private FullHttpResponse sendImage(String image) throws IOException {
        // Get image
        Path imagesFolder = webConfig.getImagesFolder();
        Path path = imagesFolder.resolve(image).normalize();
        if (!path.startsWith(imagesFolder) || !Files.exists(path)) {
            return send404();
        }

        // Send
        try (InputStream stream = Files.newInputStream(path)) {
            ByteBuf buffer = toBuffer(stream);
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    buffer);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "image/jpeg");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buffer.writerIndex());

            return response;
        }
    }

    private ByteBuf toBuffer(InputStream stream) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        int written = buffer.writeBytes(stream, 4096);
        while (written == 4096) {
            written = buffer.writeBytes(stream, 4096);
        }
        return buffer;
    }

    /**
     * Finds the corresponding file for the given URL. Changes
     * "/foo/bar.html?baz=bat" to "foo/bar.html", and "/" to "index.html".
     *
     * @param uri
     *            The URL.
     * @return The file name.
     */
    private String toFile(String uri) {
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }

        // Remove everything after ?
        int questionMarkIndex = uri.indexOf('?');
        if (questionMarkIndex >= 0) {
            uri = uri.substring(0, questionMarkIndex);
        }

        if (uri.isEmpty()) {
            return "index.html";
        }
        return uri;
    }
}
