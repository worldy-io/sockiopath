package io.worldy.sockiopath.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.StartServerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class UdpServer implements SockiopathServer {

    private static Logger logger = LoggerFactory.getLogger(SockiopathServer.class);

    private final ChannelHandler channelHandler;
    private final ExecutorService executorService;
    private final int port;

    private ChannelFuture closeFuture;
    private int actualPort;

    public UdpServer(
            ChannelHandler channelHandler,
            ExecutorService executorService,
            int port
    ) {
        this.channelHandler = channelHandler;
        this.executorService = executorService;
        this.port = port;
    }

    @Override
    public CompletableFuture<StartServerResult> start() {
        CompletableFuture<StartServerResult> future = new CompletableFuture<>();
        executorService.submit(() -> {
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                        .channel(NioDatagramChannel.class)
                        .option(ChannelOption.SO_BROADCAST, true)
                        .handler(channelHandler);

                Channel channel = bootstrap.bind(port).sync().channel();
                closeFuture = channel.closeFuture();
                actualPort = SockiopathServer.getPort(channel);
                future.complete(new StartServerResult(actualPort, closeFuture, this));
                closeFuture.await();
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            } finally {
                shutdownEventLoops(List.of(group));
            }
        });
        return future;
    }

    @Override
    public int actualPort() {
        return actualPort;
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public ChannelFuture getCloseFuture() {
        return closeFuture;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
