package io.worldy.sockiopath.websocket;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.worldy.sockiopath.CountDownLatchChannelHandler;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.websocket.client.BootstrappedWebSocketClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.BindException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WebSocketServerTest {

    @Test
    void startServerTest() throws InterruptedException, ExecutionException {

        CountDownLatch latch = new CountDownLatch(1);
        Map<Long, Object> responseMap = new HashMap<>();
        String expectedResponse = "test";


        SockiopathServer webSocketServer = getWebSocketServer(0, null);

        int port = webSocketServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get().port();

        BootstrappedWebSocketClient client = getClient(latch, responseMap, port, null);

        client.startup();
        if (!client.getChannel().writeAndFlush(new TextWebSocketFrame("test")).await(1000, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Client took too long to send a message.");
        }

        if (!latch.await(3000, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Server took too long to respond.");
        }

        TextWebSocketFrame textWebSocketFrame = (TextWebSocketFrame) responseMap.get(1l);
        assertEquals(expectedResponse, textWebSocketFrame.text());
        assertEquals(port, webSocketServer.actualPort());
    }


    @Test
    void startSslServerFailsWithMocksTest() throws InterruptedException, ExecutionException {

        CountDownLatch latch = new CountDownLatch(1);
        Map<Long, Object> responseMap = new HashMap<>();
        String expectedResponse = "test";

        SslContext serverSslContext = Mockito.mock(SslContext.class);
        SslHandler channelHandler = Mockito.mock(SslHandler.class);

        Mockito.when(
                serverSslContext.newHandler(Mockito.any(ByteBufAllocator.class), Mockito.any(ExecutorService.class))
        ).thenReturn(channelHandler);

        SockiopathServer webSocketServer = getWebSocketServer(0, serverSslContext);

        int port = webSocketServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get().port();

        BootstrappedWebSocketClient client = getClient(latch, responseMap, port, Mockito.mock(SslContext.class));

        ChannelException channelException = assertThrows(ChannelException.class, client::startup);

        assertEquals("Handshake took too long", channelException.getMessage());
    }


    @Test
    void connectTimeoutTest() throws InterruptedException, ExecutionException {
        SockiopathServer webSocketServer = getWebSocketServer(0, null);

        int port = webSocketServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get().port();

        BootstrappedWebSocketClient client = new BootstrappedWebSocketClient(
                "localhost",
                port,
                "/websocket",
                channelEchoHandler(),
                null,
                0,
                500
        );


        Exception exception = assertThrows(ChannelException.class, client::startup);
        assertEquals("Client took too long to connect", exception.getMessage());
    }

    @Test
    void handshakeTimeoutTest() throws InterruptedException, ExecutionException {
        SockiopathServer webSocketServer = getWebSocketServer(0, null);

        int port = webSocketServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get().port();

        BootstrappedWebSocketClient client = new BootstrappedWebSocketClient(
                "localhost",
                port,
                "/websocket",
                channelEchoHandler(),
                null,
                500,
                0
        );


        Exception exception = assertThrows(ChannelException.class, client::startup);
        assertEquals("Handshake took too long", exception.getMessage());
    }

    @Test
    void bindPortException() {
        SockiopathServer webSocketServer = getWebSocketServer(1, null);

        Exception exception = assertThrows(ExecutionException.class, () -> {
            webSocketServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get();
        });

        assertThat(exception.getCause(), instanceOf(BindException.class));
    }

    private static WebSocketServer getWebSocketServer(int port, SslContext serverSslContext) {
        final ChannelInitializer<SocketChannel> basicWebSocketChannelHandler;
        if (serverSslContext == null) {
            basicWebSocketChannelHandler = SockiopathServer.basicWebSocketChannelHandler(WebSocketServerTest::channelEchoHandler);
        } else {
            basicWebSocketChannelHandler = SockiopathServer.basicWebSocketChannelHandler(WebSocketServerTest::channelEchoHandler, serverSslContext);
        }
        return new WebSocketServer(
                basicWebSocketChannelHandler,
                Executors.newFixedThreadPool(1),
                port
        );
    }

    private static BootstrappedWebSocketClient getClient(CountDownLatch latch, Map<Long, Object> responseMap, int port, SslContext clientSslContext) {
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

    public static SimpleChannelInboundHandler<Object> channelEchoHandler() {
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