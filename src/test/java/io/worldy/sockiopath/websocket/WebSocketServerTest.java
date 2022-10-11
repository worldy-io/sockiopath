package io.worldy.sockiopath.websocket;

import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.worldy.sockiopath.websocket.client.BootstrappedWebSocketClient;
import org.junit.jupiter.api.Test;

import java.net.BindException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
        Map<Long, TextWebSocketFrame> responseMap = new HashMap<>();

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

        assertEquals(expectedResponse, responseMap.get(1l).text());
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

    public static class CountDownLatchChannelHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

        private final CountDownLatch latch;
        private final Map<Long, TextWebSocketFrame> responseMap;
        private final Consumer<String> debug;

        public CountDownLatchChannelHandler(CountDownLatch latch, Map<Long, TextWebSocketFrame> responseMap, Consumer<String> debug) {
            this.latch = latch;
            this.responseMap = responseMap;
            this.debug = debug;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {

            if (msg instanceof TextWebSocketFrame frame) {
                debug.accept(frame.copy().text());
                responseMap.put(latch.getCount(), frame.copy());
                latch.countDown();
            } else {
                throw new RuntimeException("Unexpected message received!");
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }


    private static SimpleChannelInboundHandler<WebSocketFrame> channelEchoHandler() {
        return new SimpleChannelInboundHandler<>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, WebSocketFrame webSocketFrame) {
                if (webSocketFrame instanceof TextWebSocketFrame textFrame) {
                    String textMessage = textFrame.text();
                    channelHandlerContext.channel().writeAndFlush(new TextWebSocketFrame(textMessage));
                }
            }
        };
    }


}