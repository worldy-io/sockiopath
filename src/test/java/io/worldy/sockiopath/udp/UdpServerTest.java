package io.worldy.sockiopath.udp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.worldy.sockiopath.CountDownLatchChannelHandler;
import io.worldy.sockiopath.udp.client.BootstrappedUdpClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

public class UdpServerTest {

    @Test
    void startServerTest() throws InterruptedException, ExecutionException {
        UdpServer udpServer = new UdpServer(
                getEchoChannelHandler(),
                Executors.newFixedThreadPool(1),
                0
        );

        int port = udpServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get().port();
        String expectedResponse = "hello";
        String response = request("hello", expectedResponse.getBytes().length, port);
        assertEquals(expectedResponse, response);
        assertEquals(port, udpServer.actualPort());
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
        assertEquals(expectedResponse, UdpServer.byteBufferToString(datagramPacket.content().nioBuffer()));
    }

    @Test
    void bindPortException() {
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

    public static String request(String request, int expectedResponseSize, int port) {
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
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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