package io.worldy.sockiopath.websocket;

import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.worldy.sockiopath.CountDownLatchChannelHandler;
import io.worldy.sockiopath.websocket.client.BootstrappedWebSocketClient;
import org.junit.jupiter.api.Test;

import java.net.BindException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WebSocketServerTest {

    @Test
    void startServerTest() throws InterruptedException, ExecutionException {
        WebSocketServer webSocketServer = new WebSocketServer(
                WebSocketServer.basicChannelHandler(channelEchoHandler(), null),
                Executors.newFixedThreadPool(1),
                0
        );

        int port = webSocketServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get().port();
        String expectedResponse = "test";

        CountDownLatch latch = new CountDownLatch(1);
        Map<Long, Object> responseMap = new HashMap<>();

        BootstrappedWebSocketClient client = new BootstrappedWebSocketClient(
                "localhost",
                port,
                "/websocket",
                new CountDownLatchChannelHandler(latch, responseMap, (message) -> {
                }),
                null,
                500,
                500
        );

        client.startup();
        if (!client.getChannel().writeAndFlush(new TextWebSocketFrame("test")).await(1000, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Client took too long to send a message.");
        }

        if (!latch.await(3000, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Server took too long to respond.");
        }

        TextWebSocketFrame textWebSocketFrame = (TextWebSocketFrame) responseMap.get(1l);
        assertEquals(expectedResponse, textWebSocketFrame.text());
    }


    @Test
    void connectTimeoutTest() throws InterruptedException, ExecutionException {
        WebSocketServer webSocketServer = new WebSocketServer(
                WebSocketServer.basicChannelHandler(channelEchoHandler(), null),
                Executors.newFixedThreadPool(1),
                0
        );

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
        WebSocketServer webSocketServer = new WebSocketServer(
                WebSocketServer.basicChannelHandler(channelEchoHandler(), null),
                Executors.newFixedThreadPool(1),
                0
        );

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
        WebSocketServer webSocketServer = new WebSocketServer(
                WebSocketServer.basicChannelHandler(channelEchoHandler(), null),
                Executors.newFixedThreadPool(1),
                1
        );

        Exception exception = assertThrows(ExecutionException.class, () -> {
            webSocketServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get();
        });

        assertThat(exception.getCause(), instanceOf(BindException.class));
    }

    private static SimpleChannelInboundHandler<Object> channelEchoHandler() {
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