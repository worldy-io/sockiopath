package io.worldy.sockiopath;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.ssl.SslContext;
import io.worldy.sockiopath.websocket.WebSocketServer;
import io.worldy.sockiopath.websocket.client.BootstrappedWebSocketClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SockiopathServerTest {


    @Test
    void shutDownServerWithInterruptionTest() throws InterruptedException, ExecutionException {

        Logger logger = Mockito.mock(Logger.class);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        SockiopathServer webSocketServer = new WebSocketServer(
                SockiopathServer.basicWebSocketChannelHandler(SockiopathServerTest::channelEchoWebSocketHandler),
                executor,
                0
        ) {
            @Override
            public Logger getLogger() {
                return logger;
            }

            @Override
            public long getShutdownTimeoutMillis() {
                return 1;
            }
        };

        webSocketServer.start().get().closeFuture().cancel(false);

        executor.awaitTermination(3000, TimeUnit.MILLISECONDS);
        Mockito.verify(logger, Mockito.times(1)).debug("Interruption required!");
        Mockito.verify(logger, Mockito.times(1)).info("done shutting down ExecutorService.");
    }

    @Test
    void shutDownServerWithoutInterruptionTest() throws InterruptedException, ExecutionException {

        Logger logger = Mockito.mock(Logger.class);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        SockiopathServer webSocketServer = new WebSocketServer(
                SockiopathServer.basicWebSocketChannelHandler(SockiopathServerTest::channelEchoWebSocketHandler),
                executor,
                0
        ) {
            @Override
            public Logger getLogger() {
                return logger;
            }

            @Override
            public long getShutdownTimeoutMillis() {
                return 0;
            }
        };

        webSocketServer.start().get().closeFuture().cancel(false);

        executor.awaitTermination(3000, TimeUnit.MILLISECONDS);
        Mockito.verify(logger, Mockito.never()).debug("Interruption required!");
        Mockito.verify(logger, Mockito.times(1)).info("done shutting down ExecutorService.");
    }

    public static WebSocketServer getWebSocketServer(int port, SslContext serverSslContext) {
        final ChannelInitializer<SocketChannel> basicWebSocketChannelHandler;
        if (serverSslContext == null) {
            basicWebSocketChannelHandler = SockiopathServer.basicWebSocketChannelHandler(SockiopathServerTest::channelEchoWebSocketHandler);
        } else {
            basicWebSocketChannelHandler = SockiopathServer.basicWebSocketChannelHandler(SockiopathServerTest::channelEchoWebSocketHandler, serverSslContext);
        }
        return new WebSocketServer(
                basicWebSocketChannelHandler,
                Executors.newFixedThreadPool(1),
                port
        );
    }

    public static BootstrappedWebSocketClient getWebSocketClient(CountDownLatch latch, Map<Long, Object> responseMap, int port, SslContext clientSslContext) {
        return new BootstrappedWebSocketClient(
                "localhost",
                port,
                "/websocket",
                new CountDownLatchChannelHandler(latch, responseMap, (message) -> {
                }),
                clientSslContext,
                500,
                500
        );
    }

    public static SimpleChannelInboundHandler<Object> channelEchoWebSocketHandler() {
        return new SimpleChannelInboundHandler<>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object message) {
                if (message instanceof TextWebSocketFrame textFrame) {
                    String textMessage = textFrame.text();
                    channelHandlerContext.channel().writeAndFlush(new TextWebSocketFrame(textMessage));
                }
            }
        };
    }


}