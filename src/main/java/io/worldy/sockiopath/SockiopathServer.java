package io.worldy.sockiopath;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface SockiopathServer {

    Logger logger = LoggerFactory.getLogger(SockiopathServer.class);

    int DEFAULT_SHUTDOWN_TIMEOUT_MILLIS = 500;
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

    default void shutdownAndAwaitTermination(ExecutorService pool, List<EventLoopGroup> groups) {
        logger.info("shutting down server...");

        ListIterator<EventLoopGroup> it = groups.listIterator();
        while (it.hasNext()) {
            int index = it.nextIndex();
            EventLoopGroup group = it.next();
            logger.info("shutting down event loop group: " + index + "...");
            group.shutdownGracefully();
        }
        logger.info("done shutting down event loop groups.");

        logger.info("shutting down ExecutorService pool...");
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(getShutdownTimeoutMillis(), TimeUnit.MILLISECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(getShutdownTimeoutMillis(), TimeUnit.MILLISECONDS))
                    logger.error("ExecutorService pool did not terminate: " + this.getClass().getTypeName());
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        logger.info("done shutting down ExecutorService.");
    }

    default long getShutdownTimeoutMillis() {
        return DEFAULT_SHUTDOWN_TIMEOUT_MILLIS;
    }
}
