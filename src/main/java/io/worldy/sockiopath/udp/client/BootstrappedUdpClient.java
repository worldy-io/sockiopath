package io.worldy.sockiopath.udp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.util.concurrent.TimeUnit;


public final class BootstrappedUdpClient {

    protected final String host;
    protected final int port;
    protected final int connectTimeoutMillis;
    protected final NioEventLoopGroup workGroup;
    protected final SimpleChannelInboundHandler<Object> messageHandler;

    protected Channel channel;

    public BootstrappedUdpClient(String host, int port, SimpleChannelInboundHandler<Object> messageHandler, int connectTimeoutMillis) {
        this.host = host;
        this.port = port;
        this.messageHandler = messageHandler;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.workGroup = new NioEventLoopGroup();
    }

    public Channel getChannel() {
        return channel;
    }

    public void startup() throws InterruptedException {
        Bootstrap b = new Bootstrap();
        b.group(workGroup);
        b.channel(NioDatagramChannel.class);
        b.handler(messageHandler);
        ChannelFuture channelFuture = b.connect(host, this.port);
        if (!channelFuture.await(connectTimeoutMillis, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Client took too long to connect");
        }
        this.channel = channelFuture.channel();

    }
}
