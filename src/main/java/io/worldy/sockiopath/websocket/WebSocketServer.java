package io.worldy.sockiopath.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.StartServerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class WebSocketServer implements SockiopathServer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

    private final ChannelHandler channelHandler;
    private final ExecutorService executor;
    final int port;

    public WebSocketServer(
            ChannelHandler channelHandler,
            ExecutorService executor,
            int port
    ) {
        this.channelHandler = channelHandler;
        this.executor = executor;
        this.port = port;
    }

    public CompletableFuture<StartServerResult> start() {
        CompletableFuture<StartServerResult> future = new CompletableFuture<>();
        executor.submit(() -> {
            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(channelHandler);

                Channel channel = b.bind(port).sync().channel();
                ChannelFuture closeFuture = channel.closeFuture();
                future.complete(new StartServerResult(SockiopathServer.getPort(channel), closeFuture));
                closeFuture.await();
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        });
        return future;
    }

}