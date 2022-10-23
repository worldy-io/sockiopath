package io.worldy.sockiopath.websocket.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;


public final class BootstrappedWebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(BootstrappedWebSocketClient.class);

    protected final String host;
    protected final int port;
    protected final int connectTimeoutMillis;
    protected final String path;
    protected final SslContext sslContext;
    protected final int handshakeTimeoutMillis;
    protected final NioEventLoopGroup workGroup;
    protected final SimpleChannelInboundHandler<Object> messageHandler;

    private Channel channel;

    public BootstrappedWebSocketClient(
            String host,
            int port,
            String path,
            SimpleChannelInboundHandler<Object> messageHandler,
            SslContext sslContext,
            int connectTimeoutMillis,
            int handshakeTimeoutMillis
    ) {
        this.host = host;
        this.port = port;
        this.path = path;
        this.messageHandler = messageHandler;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.sslContext = sslContext;
        this.handshakeTimeoutMillis = handshakeTimeoutMillis;
        this.workGroup = new NioEventLoopGroup();
    }

    public void startup() {
        try {
            String scheme = sslContext != null ? "wss://" : "ws://";

            URI uri = new URI(scheme + host + path);

            WebSocketClientHandshaker handshaker =
                    WebSocketClientHandshakerFactory.newHandshaker(
                            uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders());

            HandshakerChannelHandler handshakeHandler = new HandshakerChannelHandler(handshaker, messageHandler);

            Bootstrap b = new Bootstrap();
            b.group(workGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslContext != null) {
                                p.addLast(sslContext.newHandler(ch.alloc(), host, port));
                            }
                            p.addLast(
                                    new HttpClientCodec(),
                                    new HttpObjectAggregator(8192),
                                    handshakeHandler
                            );
                        }
                    });

            ChannelFuture channelFuture = b.connect(uri.getHost(), port);
            if (!channelFuture.await(connectTimeoutMillis)) {
                throw new ChannelException("Client took too long to connect");
            }

            if (!handshakeHandler.getHandshakeFuture().await(handshakeTimeoutMillis)) {
                throw new ChannelException("Handshake took too long");
            }

            this.channel = channelFuture.channel();
        } catch (URISyntaxException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public void shutdown() {
        workGroup.shutdownGracefully();
    }
}
