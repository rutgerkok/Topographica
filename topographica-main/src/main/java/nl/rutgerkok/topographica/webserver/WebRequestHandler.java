package nl.rutgerkok.topographica.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.bukkit.plugin.Plugin;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import nl.rutgerkok.topographica.config.WebConfig;
import nl.rutgerkok.topographica.config.WebPaths;

final class WebRequestHandler {

    private static final String IMAGES_URL = "/" + WebPaths.IMAGES + "/";

    private final Plugin plugin;
    private final WebConfig webConfig;

    WebRequestHandler(Plugin plugin, WebConfig webConfig) {
        this.plugin = Objects.requireNonNull(plugin);
        this.webConfig = Objects.requireNonNull(webConfig);
    }

    public FullHttpResponse respond(FullHttpRequest request) throws IOException {
        String uri = request.uri();
        if (uri.startsWith(IMAGES_URL)) {
            return sendImage(uri.substring(IMAGES_URL.length()));
        }

        return send404();
    }

    private FullHttpResponse send404() throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        try (InputStream stream = plugin.getResource("web/404.html")) {
            buffer.writeBytes(stream, 4096);
        }

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.NOT_FOUND,
                buffer);

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=utf-8");
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
        ByteBuf buffer = Unpooled.buffer();
        try (InputStream stream = Files.newInputStream(path)) {
            buffer.writeBytes(stream, 20 * 1024);
        }
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                buffer);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "image/jpeg");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, buffer.writerIndex());

        return response;
    }
}
