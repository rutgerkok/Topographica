package nl.rutgerkok.topographica.webserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;

import com.google.common.collect.ImmutableMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import nl.rutgerkok.topographica.marker.Marker;

final class WebRequestHandler {

    private static final Pattern REPLACEMENT_PATTERN = Pattern.compile("<\\?=\\s*([A-Z_]+)\\s*\\?>");

    private static final String IMAGES_URL = "/" + WebPaths.IMAGES + "/";
    private static final byte[] NEW_LINE = new byte[] { '\r', '\n' };

    private final BundledFiles bundledFiles;
    private final Logger logger;
    private final ServerInfo serverInfo;

    WebRequestHandler(BundledFiles files, ServerInfo serverInfo, Logger logger) {
        this.bundledFiles = Objects.requireNonNull(files, "files");
        this.serverInfo = Objects.requireNonNull(serverInfo, "serverInfo");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    private String doSmartReplacements(String line, WebWorld currentWorld) throws IOException {
        Matcher matcher = REPLACEMENT_PATTERN.matcher(line);
        if (!matcher.find()) {
            return line;
        }
        switch (matcher.group(1)) {
            case "WORLD_LIST":
                StringBuffer buffer = new StringBuffer(line.substring(0, matcher.start()));
                for (WebWorld world : serverInfo.getWorlds()) {
                    buffer.append("<li><a href=\"/?world=");
                    buffer.append(Escape.forQueryParam(world.getFolderName()));
                    buffer.append('"');
                    if (world.equals(currentWorld)) {
                        buffer.append(" class=\"current\"");
                    }
                    buffer.append('>');
                    buffer.append(Escape.forHtml(world.getDisplayName()));
                    buffer.append("</a></li>\r\n");
                }
                buffer.append(line.substring(matcher.end()));
                return buffer.toString();
            case "WORLD_FOLDER_NAME":
                return line.substring(0, matcher.start()) + Escape.forQueryParam(currentWorld.getFolderName())
                + line.substring(matcher.end());
            case "WORLD_ORIGIN":
                int[] origin = currentWorld.getOrigin();
                return line.substring(0, matcher.start()) +
                        "[" + origin[0] + "," + origin[2] + "]"
                        + line.substring(matcher.end());
            case "WORLD_MARKERS":
                StringWriter writer = new StringWriter();
                writer.write(line.substring(0, matcher.start()));
                writer.write("[");
                boolean first = true;
                for (Marker marker : currentWorld.getMarkers()) {
                    if (first) {
                        first = false;
                    } else {
                        writer.write(",");
                    }
                    marker.writeJSONString(writer);
                }
                writer.write("]");
                writer.write(line.substring(matcher.end()));
                return writer.toString();
            default:
                throw new RuntimeException("Cannot translate variable " + matcher.group(1));
        }
    }

    private String getMime(String fileName) {
        fileName = fileName.toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (fileName.endsWith(".css")) {
            return "text/css";
        }
        if (fileName.endsWith(".js")) {
            return "text/javascript";
        }
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        throw new UnsupportedOperationException("Unkown MIME: " + fileName);
    }

    private WebWorld getWorldFromQuery(String uri) {
        Optional<WebWorld> world = Optional.empty();

        String query = "";
        int questionMarkIndex = uri.indexOf('?');
        if (questionMarkIndex != -1) {
            query = uri.substring(questionMarkIndex + 1);
        }
        int worldNameIndex = query.indexOf("world=");
        if (worldNameIndex != -1) {
            String worldName = query.substring(worldNameIndex + "world=".length());
            int ampersandIndex = worldName.indexOf('&');
            if (ampersandIndex != -1) {
                worldName = worldName.substring(0, ampersandIndex);
            }
            world = this.serverInfo.getWorld(worldName);
        }

        return world
                .orElseGet(() -> this.serverInfo.getWorlds().iterator().next());
    }

    private FullHttpResponse jsonResponse(List<?> output) throws IOException {
        ByteBuf buffer = Unpooled.buffer();

        try (Writer writer = new OutputStreamWriter(new ByteBufOutputStream(buffer), StandardCharsets.UTF_8)) {
            JSONArray.writeJSONString(output, writer);
        }

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                buffer);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buffer.writerIndex());

        return response;
    }

    public FullHttpResponse respond(FullHttpRequest request) throws IOException {
        try {
            String uri = request.uri();

            // Images must be sent from a folder, not from the JAR file
            if (uri.startsWith(IMAGES_URL)) {
                return sendMapImage(uri.substring(IMAGES_URL.length()));
            }

            switch (toFile(uri)) {
                case "index.html":
                    return sendHomePage(getWorldFromQuery(uri));
                case "players.json":
                    return sendPlayerList(getWorldFromQuery(uri));
                default:
                    return sendFromJarFile(toFile(uri), HttpResponseStatus.OK);
            }
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

    private FullHttpResponse sendHomePage(WebWorld world) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        try (InputStream stream = bundledFiles.getResource("web/index.html")) {
            if (stream == null) {
                return send404();
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            String line;
            while ((line = reader.readLine()) != null) {
                line = doSmartReplacements(line, world);
                buffer.writeBytes(line.getBytes(StandardCharsets.UTF_8));
                buffer.writeBytes(NEW_LINE);
            }
        }

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer);

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, getMime("index.html"));
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buffer.writerIndex());

        return response;
    }

    private FullHttpResponse sendMapImage(String image) throws IOException {
        // Get image
        Path imagesFolder = serverInfo.getImagesFolder();
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

    private FullHttpResponse sendPlayerList(WebWorld world) throws IOException {
        Collection<? extends WebPlayer> players = this.serverInfo.getPlayers(world);
        List<Map<?, ?>> output = new ArrayList<>();
        for (WebPlayer player : players) {
            long position = player.getPosition();
            output.add(ImmutableMap.of("name", player.getDisplayName(),
                    "x", IntPair.getX(position),
                    "z", IntPair.getZ(position)));
        }
        return jsonResponse(output);
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
