package io.worldy.sockiopath.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.worldy.sockiopath.AbstractSockiopathServer;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.SockiopathServerHandler;
import io.worldy.sockiopath.StartServerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class UdpServer extends AbstractSockiopathServer {

    private static Logger logger = LoggerFactory.getLogger(SockiopathServer.class);

    public UdpServer(
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

                if (channelHandler instanceof SockiopathServerHandler<?> sockiopathServerHandler) {
                    sockiopathServerHandler.setChannelPool(channelPoolInstance(bootstrap, actualPort));
                }

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

    protected ChannelPool channelPoolInstance(Bootstrap bootstrap, int actualPort) {
        bootstrap.remoteAddress("localhost", actualPort);
        return new SimpleChannelPool(bootstrap, new AbstractChannelPoolHandler() {
            @Override
            public void channelCreated(Channel channel) throws Exception {
                logger.debug("Channel created in pool");
            }
        });
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
