package io.worldy.sockiopath.udp.client;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.worldy.sockiopath.messaging.DefaultMessageParser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.Map;

import static io.smallrye.common.constraint.Assert.assertTrue;
import static io.worldy.sockiopath.SockiopathHandlerTest.getMessageHandlers;

class UdpClientHandlerTest {

    @Test
    void isNotUdpTest() {
        assertTrue(new UdpClientHandler(Map.of()).isUdp());
    }

    @Test
    void channelRead0BinaryTest() throws Exception {
        Logger loggerMock = Mockito.mock(Logger.class);
        UdpClientHandler handler =
                new UdpClientHandler(getMessageHandlers(), new DefaultMessageParser('|'), loggerMock);

        InetSocketAddress sender = Mockito.mock(InetSocketAddress.class);
        DatagramPacket packet =
                new DatagramPacket(Unpooled.wrappedBuffer("address-empty||payload".getBytes()), sender);

        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(context.channel()).thenReturn(channel);
        handler.channelRead0(context, packet);

        Mockito.verify(loggerMock, Mockito.times(1)).debug("No response from message bus: address-empty");
    }
}