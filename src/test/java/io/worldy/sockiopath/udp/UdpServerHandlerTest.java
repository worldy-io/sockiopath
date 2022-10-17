package io.worldy.sockiopath.udp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.worldy.sockiopath.SockiopathServerHandler;
import io.worldy.sockiopath.messaging.DefaultMessageParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static io.worldy.sockiopath.SockiopathServerHandlerTest.getMessageHandlers;
import static io.worldy.sockiopath.SockiopathServerHandlerTest.getSessionStore;
import static io.worldy.sockiopath.SockiopathServerHandlerTest.getUdpHandler;
import static io.worldy.sockiopath.SockiopathServerHandlerTest.verifyNoWrites;
import static io.worldy.sockiopath.SockiopathServerHandlerTest.verifyNoWritesOrFlushes;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UdpServerHandlerTest {

    @Test
    void channelRead0Test() throws Exception {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        DatagramPacket packet = Mockito.mock(DatagramPacket.class);
        SockiopathServerHandler<DatagramPacket> sockioPathServerHandler = getUdpHandler(context);

        ByteBuf content = Unpooled.wrappedBuffer("address-a|sessionId-a|data-a".getBytes());
        Mockito.when(packet.content()).thenReturn(content);
        InetSocketAddress sender = Mockito.mock(InetSocketAddress.class);
        Mockito.when(packet.sender()).thenReturn(sender);

        sockioPathServerHandler.channelRead0(context, packet);
        Mockito.verify(context, Mockito.times(1)).writeAndFlush(Mockito.any());
        assertEquals(sender, sockioPathServerHandler.getSession("sessionId-a").getUdpSocketAddress());
        assertEquals(context, sockioPathServerHandler.getSession("sessionId-a").getUdpContext());
    }

    @Test
    void channelRead0DeliminatorSpecifiedTest() throws Exception {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        DatagramPacket packet = Mockito.mock(DatagramPacket.class);
        SockiopathServerHandler<DatagramPacket> sockioPathServerHandler = new UdpServerHandler(getSessionStore(context), getMessageHandlers(), '|');

        ByteBuf content = Unpooled.wrappedBuffer("address-a|sessionId-a|data-a".getBytes());
        Mockito.when(packet.content()).thenReturn(content);
        InetSocketAddress sender = Mockito.mock(InetSocketAddress.class);
        Mockito.when(packet.sender()).thenReturn(sender);

        sockioPathServerHandler.channelRead0(context, packet);
        Mockito.verify(context, Mockito.times(1)).writeAndFlush(Mockito.any());
        assertEquals(sender, sockioPathServerHandler.getSession("sessionId-a").getUdpSocketAddress());
        assertEquals(context, sockioPathServerHandler.getSession("sessionId-a").getUdpContext());
    }

    @Test
    void channelRead0TimeoutTest() throws Exception {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        DatagramPacket packet = Mockito.mock(DatagramPacket.class);
        Logger loggerMock = Mockito.mock(Logger.class);
        SockiopathServerHandler<DatagramPacket> sockioPathServerHandler = new UdpServerHandler(getSessionStore(context), getMessageHandlers(), new DefaultMessageParser('|'), loggerMock);

        ByteBuf content = Unpooled.wrappedBuffer("address-timeout|sessionId-timeout|data-timeout".getBytes());
        Mockito.when(packet.content()).thenReturn(content);
        InetSocketAddress sender = Mockito.mock(InetSocketAddress.class);
        Mockito.when(packet.sender()).thenReturn(sender);

        sockioPathServerHandler.channelRead0(context, packet);
        verifyNoWritesOrFlushes(context);
        Mockito.verify(loggerMock, Mockito.times(1)).error(Mockito.any(), Mockito.any(TimeoutException.class));
    }

    @Test
    void channelRead0NoSessionTest() throws Exception {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        DatagramPacket packet = Mockito.mock(DatagramPacket.class);
        SockiopathServerHandler<DatagramPacket> sockioPathServerHandler = getUdpHandler(context);

        ByteBuf content = Unpooled.wrappedBuffer("address-b|sessionId-b|data-b".getBytes());
        Mockito.when(packet.content()).thenReturn(content);
        InetSocketAddress sender = Mockito.mock(InetSocketAddress.class);
        InetAddress senderAddress = Mockito.mock(InetAddress.class);
        Mockito.when(senderAddress.getCanonicalHostName()).thenReturn("client.host");
        Mockito.when(sender.getAddress()).thenReturn(senderAddress);
        Mockito.when(packet.sender()).thenReturn(sender);

        sockioPathServerHandler.channelRead0(context, packet);
        verifyNoWritesOrFlushes(context);
    }

    @Test
    void channelRead0NoMessageBusTest() throws Exception {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        DatagramPacket packet = Mockito.mock(DatagramPacket.class);
        Logger loggerMock = Mockito.mock(Logger.class);
        SockiopathServerHandler<DatagramPacket> sockioPathServerHandler = new UdpServerHandler(getSessionStore(context), getMessageHandlers(), new DefaultMessageParser('|'), loggerMock);

        ByteBuf content = Unpooled.wrappedBuffer("address_noAddress|sessionId-noAddress|data_noAddress".getBytes());
        Mockito.when(packet.content()).thenReturn(content);
        InetSocketAddress sender = Mockito.mock(InetSocketAddress.class);
        InetAddress senderAddress = Mockito.mock(InetAddress.class);
        Mockito.when(senderAddress.getCanonicalHostName()).thenReturn("client.host");
        Mockito.when(sender.getAddress()).thenReturn(senderAddress);
        Mockito.when(packet.sender()).thenReturn(sender);

        sockioPathServerHandler.channelRead0(context, packet);
        verifyNoWritesOrFlushes(context);
        Mockito.verify(loggerMock, Mockito.times(1)).debug("No message handler for: address_noAddress");
    }

    public static Stream<Arguments> channelRead0BadMessageData() {
        return Stream.of(
                Arguments.of("debugOn", true, "Unable to parse message. sender: [client.host.debugOn]. content: noAddress"),
                Arguments.of("debugOff", false, "Unable to parse message. sender: [client.host.debugOff]. content: Enable debugging to see content")
        );
    }

    @ParameterizedTest(name = "channelRead0BadMessage_{0}")
    @MethodSource("channelRead0BadMessageData")
    void channelRead0BadMessageTest(String testName, boolean debugEnabled, String expectedDebugLog) throws Exception {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        DatagramPacket packet = Mockito.mock(DatagramPacket.class);
        Logger loggerMock = Mockito.mock(Logger.class);
        Mockito.when(loggerMock.isDebugEnabled()).thenReturn(debugEnabled);
        SockiopathServerHandler<DatagramPacket> sockioPathServerHandler = new UdpServerHandler(null, null, new DefaultMessageParser('|'), loggerMock);

        ByteBuf content = Unpooled.wrappedBuffer("noAddress".getBytes());
        Mockito.when(packet.content()).thenReturn(content);
        InetSocketAddress sender = Mockito.mock(InetSocketAddress.class);
        InetAddress senderAddress = Mockito.mock(InetAddress.class);
        Mockito.when(senderAddress.getCanonicalHostName()).thenReturn("client.host." + testName);
        Mockito.when(sender.getAddress()).thenReturn(senderAddress);
        Mockito.when(packet.sender()).thenReturn(sender);

        sockioPathServerHandler.channelRead0(context, packet);
        verifyNoWritesOrFlushes(context);
        Mockito.verify(loggerMock, Mockito.times(1)).error(expectedDebugLog);
    }

    @Test
    void channelRegistered() throws Exception {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        SockiopathServerHandler<DatagramPacket> handler = getUdpHandler(context);
        handler.channelRegistered(context);
        Mockito.verify(context, Mockito.times(1)).fireChannelRegistered();
        assertEquals(context, handler.getChannelHandlerContext());
        verifyNoWrites(context);
    }

    @Test
    void channelReadCompleteTest() {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        getUdpHandler(context).channelReadComplete(context);
        Mockito.verify(context, Mockito.times(1)).flush();
        verifyNoWrites(context);
    }

    @Test
    void exceptionCaughtTest() {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        Logger loggerMock = Mockito.mock(Logger.class);
        SockiopathServerHandler<DatagramPacket> sockioPathServerHandler = new UdpServerHandler(null, null, new DefaultMessageParser('|'), loggerMock);
        RuntimeException runtimeException = new RuntimeException("runtimeException message");
        sockioPathServerHandler.exceptionCaught(context, runtimeException);

        Mockito.verify(loggerMock, Mockito.times(1)).error("Error handling connection: runtimeException message", runtimeException);
        Mockito.verify(context, Mockito.never()).close();
    }
}