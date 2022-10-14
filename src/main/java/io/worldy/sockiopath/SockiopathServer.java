package io.worldy.sockiopath;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public interface SockiopathServer {

    int DEFAULT_MAX_CONTENT_LENGTH = 65536;
    String DEFAULT_WEB_SOCKET_PATH = "/websocket";

    CompletableFuture<StartServerResult> start();

    int actualPort();

    static int getPort(Channel channel) {
        SocketAddress socketAddress = channel.localAddress();
        return ((InetSocketAddress) socketAddress).getPort();
    }

    static ChannelInitializer<SocketChannel> basicWebSocketChannelHandler(
            Supplier<SimpleChannelInboundHandler<?>> messageHandlerSupplier
    ) {
        return basicWebSocketChannelHandler(messageHandlerSupplier, null);
    }

    static ChannelInitializer<SocketChannel> basicWebSocketChannelHandler(
            Supplier<SimpleChannelInboundHandler<?>> messageHandlerSupplier, SslContext sslCtx
    ) {
        return basicWebSocketChannelHandler(List.of(messageHandlerSupplier), sslCtx);
    }

    static ChannelInitializer<SocketChannel> basicWebSocketChannelHandler(
            List<Supplier<SimpleChannelInboundHandler<?>>> messageHandlers,
            SslContext sslCtx
    ) {
        return basicWebSocketChannelHandler(DEFAULT_WEB_SOCKET_PATH, DEFAULT_MAX_CONTENT_LENGTH, messageHandlers, sslCtx);
    }

    static ChannelInitializer<SocketChannel> basicWebSocketChannelHandler(
            String path,
            int maxContentLength,
            List<Supplier<SimpleChannelInboundHandler<?>>> messageHandlers,
            SslContext sslCtx
    ) {
        ExecutorService sslChannelExecutor = Executors.newFixedThreadPool(1);
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) {
                ChannelPipeline pipeline = socketChannel.pipeline();
                if (sslCtx != null) {
                    pipeline.addLast(sslCtx.newHandler(socketChannel.alloc(), sslChannelExecutor));
                }
                pipeline.addLast(new HttpServerCodec());
                pipeline.addLast(new HttpObjectAggregator(maxContentLength));
                pipeline.addLast(new WebSocketServerProtocolHandler(path, null, true));
                messageHandlers.forEach(messageHandlerSupplier -> pipeline.addLast(messageHandlerSupplier.get()));
            }
        };
    }
}
