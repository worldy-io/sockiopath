package io.worldy.sockiopath.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.StartServerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class UdpServer implements SockiopathServer {

    private static final Logger logger = LoggerFactory.getLogger(UdpServer.class);

    private final ChannelHandler channelHandler;
    private final ExecutorService executor;
    private final int port;

    private GenericFutureListener<Future<? super Void>> onClose;


    public UdpServer(
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
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                        .channel(NioDatagramChannel.class)
                        .option(ChannelOption.SO_BROADCAST, true)
                        .handler(channelHandler);

                Channel channel = bootstrap.bind(port).sync().channel();
                ChannelFuture closeFuture = channel.closeFuture();
                future.complete(new StartServerResult(SockiopathServer.getPort(channel), closeFuture));
                closeFuture.await();
            } catch (Exception ex) {
                future.completeExceptionally(ex);
            } finally {
                group.shutdownGracefully();
            }
        });
        return future;
    }

    public static String byteBufferToString(ByteBuffer content) {
        var capacity = content.capacity();
        content.position(0);

        StringBuilder builder = new StringBuilder();

        while (content.position() < capacity) {
            byte singleByte = content.get();
            char character = (char) singleByte;
            builder.append(character);
        }
        return builder.toString();
    }
}