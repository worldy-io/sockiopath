package io.worldy.sockiopath.udp;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
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