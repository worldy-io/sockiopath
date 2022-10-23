package io.worldy.sockiopath.websocket;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelException;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.StartServerResult;
import io.worldy.sockiopath.websocket.client.BootstrappedWebSocketClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.BindException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.worldy.sockiopath.SockiopathServerTest.channelEchoWebSocketHandler;
import static io.worldy.sockiopath.SockiopathServerTest.getWebSocketClient;
import static io.worldy.sockiopath.SockiopathServerTest.getWebSocketServer;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class WebSocketServerTest {

    @Test
    void startServerTest() throws InterruptedException, ExecutionException, TimeoutException {

        CountDownLatch latch = new CountDownLatch(1);
        Map<Long, Object> responseMap = new HashMap<>();
        String expectedResponse = "test";


        SockiopathServer webSocketServer = getWebSocketServer(0, null);

        StartServerResult startServerResult = webSocketServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get();
        int port = startServerResult.port();

        BootstrappedWebSocketClient client = getWebSocketClient(latch, responseMap, port, null);

        //client.startup().orTimeout(1000, TimeUnit.MILLISECONDS).get();
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

        if (!startServerResult.closeFuture().cancel(true)) {
            fail("unable to stop server.");
        }
        if (!startServerResult.closeFuture().await(1000, TimeUnit.MILLISECONDS)) {
            fail("server took too long to shut down.");
        }
    }


    @Test
    void startSslServerFailsWithMocksTest() throws InterruptedException, ExecutionException {

        CountDownLatch latch = new CountDownLatch(1);
        Map<Long, Object> responseMap = new HashMap<>();

        SslContext serverSslContext = Mockito.mock(SslContext.class);
        SslHandler channelHandler = Mockito.mock(SslHandler.class);

        Mockito.when(
                serverSslContext.newHandler(Mockito.any(ByteBufAllocator.class), Mockito.any(ExecutorService.class))
        ).thenReturn(channelHandler);

        SockiopathServer webSocketServer = getWebSocketServer(0, serverSslContext);

        int port = webSocketServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get().port();

        BootstrappedWebSocketClient client = getWebSocketClient(latch, responseMap, port, Mockito.mock(SslContext.class));

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
                channelEchoWebSocketHandler(),
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
                channelEchoWebSocketHandler(),
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


}