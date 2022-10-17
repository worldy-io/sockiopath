package io.worldy.sockiopath.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.channel.socket.DatagramPacket;
import io.worldy.sockiopath.CountDownLatchChannelHandler;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.SockiopathServerHandlerTest;
import io.worldy.sockiopath.StartServerResult;
import io.worldy.sockiopath.messaging.DefaultMessageParser;
import io.worldy.sockiopath.session.SockiopathSession;
import io.worldy.sockiopath.udp.client.BootstrappedUdpClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.worldy.sockiopath.SockiopathHandlerTest.getMessageHandlers;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class UdpServerTest {

    @Test
    void startServerTest() throws InterruptedException, ExecutionException, TimeoutException {
        UdpServer udpServer = new UdpServer(
                getEchoChannelHandler(),
                Executors.newFixedThreadPool(1),
                0
        );

        StartServerResult startServerResult = udpServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get();
        int port = startServerResult.port();
        String expectedResponse = "hello";
        String response = request("hello", expectedResponse.getBytes().length, port, 1000);
        assertEquals(expectedResponse, response);
        assertEquals(port, udpServer.actualPort());

        if (!startServerResult.closeFuture().cancel(true)) {
            fail("unable to stop server.");
        }
        if (!startServerResult.closeFuture().await(1000, TimeUnit.MILLISECONDS)) {
            fail("server took too long to shut down.");
        }
    }


    @Test
    void channelPoolingTest() throws InterruptedException, ExecutionException, TimeoutException {

        Map<String, SockiopathSession> sessionMap = Map.of(
                "sessionId-a", new SockiopathSession(null)
        );
        UdpServer udpServer = new UdpServer(
                new UdpServerHandler(SockiopathServerHandlerTest.getSessionStore(sessionMap), getMessageHandlers()),
                Executors.newFixedThreadPool(1),
                0
        );

        StartServerResult startServerResult = udpServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get();
        int port = startServerResult.port();
        String expectedResponse = "response-a";
        String response = request("address-a|sessionId-a|data-a", expectedResponse.getBytes().length, port, 2000);
        assertEquals(expectedResponse, response);
        assertEquals(port, udpServer.actualPort());

        if (!startServerResult.closeFuture().cancel(true)) {
            fail("unable to stop server.");
        }
        if (!startServerResult.closeFuture().await(1000, TimeUnit.MILLISECONDS)) {
            fail("server took too long to shut down.");
        }
    }

    @Test
    void channelPoolFailureTest() throws InterruptedException, ExecutionException {
        Logger loggerMock = Mockito.mock(Logger.class);

        Map<String, SockiopathSession> sessionMap = Map.of(
                "sessionId-a", new SockiopathSession(null)
        );
        UdpServer udpServer = new UdpServer(
                new UdpServerHandler(
                        SockiopathServerHandlerTest.getSessionStore(sessionMap),
                        getMessageHandlers(),
                        new DefaultMessageParser('|'),
                        loggerMock
                ),
                Executors.newFixedThreadPool(1),
                0
        ) {
            @Override
            protected ChannelPool channelPoolInstance(Bootstrap bootstrap, int actualPort) {
                return new SimpleChannelPool(bootstrap, new AbstractChannelPoolHandler() {
                    @Override
                    public void channelCreated(Channel channel) throws Exception {
                        loggerMock.debug("Channel created in pool");
                    }
                });
            }
        };

        StartServerResult startServerResult = udpServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get();
        int port = startServerResult.port();
        String expectedResponse = "hello";
        assertThrows(TimeoutException.class, () -> request("hello", expectedResponse.getBytes().length, port, 1000));
        Mockito.verify(loggerMock, Mockito.times(1)).error("Error acquiring channel from pool. remoteAddress not set");

        if (!startServerResult.closeFuture().cancel(true)) {
            fail("unable to stop server.");
        }
        if (!startServerResult.closeFuture().await(1000, TimeUnit.MILLISECONDS)) {
            fail("server took too long to shut down.");
        }
    }

    @Test
    void bootstrappedClientTest() throws InterruptedException, ExecutionException {
        UdpServer udpServer = new UdpServer(
                getEchoChannelHandler(),
                Executors.newFixedThreadPool(1),
                0
        );

        int port = udpServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get().port();
        String expectedResponse = "test";

        CountDownLatch latch = new CountDownLatch(1);
        Map<Long, Object> responseMap = new HashMap<>();

        BootstrappedUdpClient client = new BootstrappedUdpClient(
                "localhost",
                port,
                new CountDownLatchChannelHandler(latch, responseMap, (message) -> {
                }),
                500
        );

        client.startup();

        ByteBuf message = Unpooled.wrappedBuffer("test".getBytes());
        if (!client.getChannel().writeAndFlush(message).await(1000, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Client took too long to send a message.");
        }

        if (!latch.await(1000, TimeUnit.MILLISECONDS)) {
            throw new RuntimeException("Server took too long to respond.");
        }

        DatagramPacket datagramPacket = (DatagramPacket) responseMap.get(1l);
        assertEquals(expectedResponse, SockiopathServer.byteBufferToString(datagramPacket.content().nioBuffer()));
    }

    @Test
    void bindPortExceptionTest() {
        UdpServer udpServer = new UdpServer(
                getEchoChannelHandler(),
                Executors.newFixedThreadPool(1),
                1
        );

        Exception exception = assertThrows(ExecutionException.class, () -> {
            udpServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get();
        });

        assertThat(exception.getCause(), instanceOf(BindException.class));
    }

    private static String request(String request, int expectedResponseSize, int port) {
        try (DatagramSocket socket = new DatagramSocket()) {

            InetAddress host = InetAddress.getByName("localhost");

            byte[] requestPayload = request.getBytes();
            java.net.DatagramPacket packet
                    = new java.net.DatagramPacket(requestPayload, requestPayload.length, host, port);
            socket.send(packet);

            byte[] receivedBytes = new byte[expectedResponseSize];
            packet = new java.net.DatagramPacket(receivedBytes, receivedBytes.length);
            socket.receive(packet);

            return new String(packet.getData());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String request(String request, int expectedResponseSize, int port, int timeout) throws ExecutionException, InterruptedException, TimeoutException {
        return Executors.newFixedThreadPool(1)
                .submit(() -> request(request, expectedResponseSize, port))
                .get(timeout, TimeUnit.MILLISECONDS);
    }

    private static SimpleChannelInboundHandler<DatagramPacket> getEchoChannelHandler() {
        return new SimpleChannelInboundHandler<>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
                channelHandlerContext.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(datagramPacket.copy().content()), datagramPacket.sender()));
            }
        };
    }

}