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
import java.util.concurrent.CompletableFuture;

public interface SockiopathServer {

    CompletableFuture<StartServerResult> start();

    static int getPort(Channel channel) {
        SocketAddress socketAddress = channel.localAddress();
        return ((InetSocketAddress) socketAddress).getPort();
    }

    static ChannelInitializer<SocketChannel> basicChannelHandler(
            SimpleChannelInboundHandler<Object> messageHandler,
            SslContext sslCtx
    ) {
        return basicChannelHandler("/websocket", 65536, messageHandler, sslCtx);
    }

    static ChannelInitializer<SocketChannel> basicChannelHandler(
            String path,
            int maxContentLength,
            SimpleChannelInboundHandler<Object> messageHandler,
            SslContext sslCtx
    ) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) {
                ChannelPipeline pipeline = socketChannel.pipeline();
                if (sslCtx != null) {
                    pipeline.addLast(sslCtx.newHandler(socketChannel.alloc()));
                }
                pipeline.addLast(new HttpServerCodec());
                pipeline.addLast(new HttpObjectAggregator(maxContentLength));
                pipeline.addLast(new WebSocketServerProtocolHandler(path, null, true));
                pipeline.addLast(messageHandler);
            }
        };
    }
}
