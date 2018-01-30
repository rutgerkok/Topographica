package nl.rutgerkok.topographica;

import static io.netty.buffer.Unpooled.copiedBuffer;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class WebRequestHandler {

    public FullHttpResponse respond(FullHttpRequest request) {
        final String responseMessage = "Hello from Netty!";

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                copiedBuffer(responseMessage.getBytes()));


        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, responseMessage.length());

        return response;
    }
}
