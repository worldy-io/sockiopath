package io.worldy.sockiopath.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.worldy.sockiopath.AbstractSockiopathServer;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.StartServerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class WebSocketServer extends AbstractSockiopathServer {

    private static Logger logger = LoggerFactory.getLogger(SockiopathServer.class);

    public WebSocketServer(
            ChannelHandler channelHandler,
            ExecutorService executorService,
            int port
    ) {
        super(channelHandler, executorService, port);
    }

    @Override
    public CompletableFuture<StartServerResult> start() {
        CompletableFuture<StartServerResult> future = new CompletableFuture<>();
        executorService.submit(() -> {
            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(channelHandler);

                Channel channel = b.bind(port).sync().channel();
                this.closeFuture = channel.closeFuture();
                actualPort = SockiopathServer.getPort(channel);
                future.complete(new StartServerResult(actualPort, closeFuture, this));
                closeFuture.await();
            } catch (Exception e) {
                future.completeExceptionally(e);
            } finally {
                shutdownEventLoops(List.of(bossGroup, workerGroup));
            }
        });
        return future;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
