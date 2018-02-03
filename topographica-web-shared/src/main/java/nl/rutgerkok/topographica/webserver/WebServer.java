package nl.rutgerkok.topographica.webserver;

import static io.netty.buffer.Unpooled.copiedBuffer;

import java.net.BindException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;

public final class WebServer {

    private ChannelFuture channel;
    private final EventLoopGroup masterGroup;
    private final EventLoopGroup slaveGroup;
    private final WebRequestHandler requestHandler;

    public WebServer(BundledFiles files, WebConfigInterface webConfig, Logger logger) throws BindException {
        masterGroup = new NioEventLoopGroup();
        slaveGroup = new NioEventLoopGroup();
        requestHandler = new WebRequestHandler(files, webConfig, logger);

        enable(webConfig);
    }

    public void disable() {
        slaveGroup.shutdownGracefully();
        masterGroup.shutdownGracefully();

        try {
            if (channel != null) {
                channel.channel().closeFuture().sync();
            }
        } catch (InterruptedException e) {
        }
    }

    private void enable(WebConfigInterface config) throws BindException {

        try {
            final ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(masterGroup, slaveGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() // #4
                    {
                        @Override
                        public void initChannel(final SocketChannel ch)
                                throws Exception {
                            ch.pipeline().addLast("codec", new HttpServerCodec());
                            ch.pipeline().addLast("aggregator",
                                    new HttpObjectAggregator(512 * 1024));
                            ch.pipeline().addLast("request",
                                    new ChannelInboundHandlerAdapter() {
                                        @Override
                                        public void channelRead(ChannelHandlerContext ctx, Object msg)
                                                throws Exception {
                                            try {
                                                if (msg instanceof FullHttpRequest) {
                                                    FullHttpRequest request = (FullHttpRequest) msg;
                                                    FullHttpResponse response = requestHandler.respond(request);
                                                    if (HttpUtil.isKeepAlive(request)) {
                                                        response.headers().set(HttpHeaderNames.CONNECTION,
                                                                HttpHeaderValues.KEEP_ALIVE);
                                                    }
                                                    ctx.writeAndFlush(response);
                                                }
                                            } finally {
                                                ReferenceCountUtil.release(msg);
                                            }
                                        }

                                        @Override
                                        public void channelReadComplete(ChannelHandlerContext ctx)
                                                throws Exception {
                                            ctx.flush();
                                        }

                                        @Override
                                        public void exceptionCaught(ChannelHandlerContext ctx,
                                                Throwable cause) throws Exception {
                                            ctx.writeAndFlush(new DefaultFullHttpResponse(
                                                    HttpVersion.HTTP_1_1,
                                                    HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                                    copiedBuffer(cause.getMessage().getBytes(StandardCharsets.UTF_8))));
                                        }
                                    });
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            channel = bootstrap.bind(config.getPort()).sync();
        } catch (InterruptedException e) {
        }
    }

}
