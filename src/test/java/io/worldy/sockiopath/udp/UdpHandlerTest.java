package io.worldy.sockiopath.udp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.worldy.sockiopath.messaging.DefaultMessageParser;
import io.worldy.sockiopath.messaging.MessageBus;
import io.worldy.sockiopath.session.MapBackedSessionStore;
import io.worldy.sockiopath.session.SessionStore;
import io.worldy.sockiopath.session.SockiopathSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UdpHandlerTest {

    @Test
    void channelRead0Test() {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        DatagramPacket packet = Mockito.mock(DatagramPacket.class);
        UdpHandler udpHandler = getHandler(context);

        ByteBuf content = Unpooled.wrappedBuffer("address-a|sessionId-a|data-a".getBytes());
        Mockito.when(packet.content()).thenReturn(content);
        InetSocketAddress sender = Mockito.mock(InetSocketAddress.class);
        Mockito.when(packet.sender()).thenReturn(sender);

        udpHandler.channelRead0(context, packet);
        Mockito.verify(context, Mockito.times(1)).writeAndFlush(Mockito.any());
        assertEquals(sender, udpHandler.sessionStore.get().apply("sessionId-a").getUdpSocketAddress());
        assertEquals(context, udpHandler.sessionStore.get().apply("sessionId-a").getUdpContext());
    }

    @Test
    void channelRead0TimeoutTest() {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        DatagramPacket packet = Mockito.mock(DatagramPacket.class);
        Logger loggerMock = Mockito.mock(Logger.class);
        UdpHandler udpHandler = new UdpHandler(getSessionStore(context), getMessageHandlers(), new DefaultMessageParser('|'), loggerMock);

        ByteBuf content = Unpooled.wrappedBuffer("address-timeout|sessionId-timeout|data-timeout".getBytes());
        Mockito.when(packet.content()).thenReturn(content);
        InetSocketAddress sender = Mockito.mock(InetSocketAddress.class);
        Mockito.when(packet.sender()).thenReturn(sender);

        udpHandler.channelRead0(context, packet);
        verifyNoWritesOrFlushes(context);
        Mockito.verify(loggerMock, Mockito.times(1)).error(Mockito.any(), Mockito.any(TimeoutException.class));
    }

    @Test
    void channelRead0NoSessionTest() {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        DatagramPacket packet = Mockito.mock(DatagramPacket.class);
        UdpHandler udpHandler = getHandler(context);

        ByteBuf content = Unpooled.wrappedBuffer("address-b|sessionId-b|data-b".getBytes());
        Mockito.when(packet.content()).thenReturn(content);
        InetSocketAddress sender = Mockito.mock(InetSocketAddress.class);
        InetAddress senderAddress = Mockito.mock(InetAddress.class);
        Mockito.when(senderAddress.getCanonicalHostName()).thenReturn("client.host");
        Mockito.when(sender.getAddress()).thenReturn(senderAddress);
        Mockito.when(packet.sender()).thenReturn(sender);

        udpHandler.channelRead0(context, packet);
        verifyNoWritesOrFlushes(context);
    }

    @Test
    void channelRead0NoMessageBusTest() {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        DatagramPacket packet = Mockito.mock(DatagramPacket.class);
        Logger loggerMock = Mockito.mock(Logger.class);
        UdpHandler udpHandler = new UdpHandler(getSessionStore(context), getMessageHandlers(), new DefaultMessageParser('|'), loggerMock);

        ByteBuf content = Unpooled.wrappedBuffer("address_noAddress|sessionId-noAddress|data_noAddress".getBytes());
        Mockito.when(packet.content()).thenReturn(content);
        InetSocketAddress sender = Mockito.mock(InetSocketAddress.class);
        InetAddress senderAddress = Mockito.mock(InetAddress.class);
        Mockito.when(senderAddress.getCanonicalHostName()).thenReturn("client.host");
        Mockito.when(sender.getAddress()).thenReturn(senderAddress);
        Mockito.when(packet.sender()).thenReturn(sender);

        udpHandler.channelRead0(context, packet);
        verifyNoWritesOrFlushes(context);
        Mockito.verify(loggerMock, Mockito.times(1)).debug("No message handler for: address_noAddress");
    }

    public static Stream<Arguments> channelRead0BadMessageData() {
        return Stream.of(
                Arguments.of("debugOn", true, "Unable to parse UDP message. sender: [client.host.debugOn]. content: noAddress"),
                Arguments.of("debugOff", false, "Unable to parse UDP message. sender: [client.host.debugOff]. content: Enable debugging to see content")
        );
    }

    @ParameterizedTest(name = "channelRead0BadMessage_{0}")
    @MethodSource("channelRead0BadMessageData")
    void channelRead0BadMessageTest(String testName, boolean debugEnabled, String expectedDebugLog) {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        DatagramPacket packet = Mockito.mock(DatagramPacket.class);
        Logger loggerMock = Mockito.mock(Logger.class);
        Mockito.when(loggerMock.isDebugEnabled()).thenReturn(debugEnabled);
        UdpHandler udpHandler = new UdpHandler(null, null, new DefaultMessageParser('|'), loggerMock);

        ByteBuf content = Unpooled.wrappedBuffer("noAddress".getBytes());
        Mockito.when(packet.content()).thenReturn(content);
        InetSocketAddress sender = Mockito.mock(InetSocketAddress.class);
        InetAddress senderAddress = Mockito.mock(InetAddress.class);
        Mockito.when(senderAddress.getCanonicalHostName()).thenReturn("client.host." + testName);
        Mockito.when(sender.getAddress()).thenReturn(senderAddress);
        Mockito.when(packet.sender()).thenReturn(sender);

        udpHandler.channelRead0(context, packet);
        verifyNoWritesOrFlushes(context);
        Mockito.verify(loggerMock, Mockito.times(1)).error(expectedDebugLog);
    }

    @Test
    void channelRegistered() throws Exception {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        UdpHandler handler = getHandler(context);
        handler.channelRegistered(context);
        Mockito.verify(context, Mockito.times(1)).fireChannelRegistered();
        assertEquals(context, handler.getChannelHandlerContext());
        verifyNoWrites(context);
    }

    @Test
    void channelReadCompleteTest() {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        getHandler(context).channelReadComplete(context);
        Mockito.verify(context, Mockito.times(1)).flush();
        verifyNoWrites(context);
    }

    @Test
    void exceptionCaughtTest() {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        Logger loggerMock = Mockito.mock(Logger.class);
        UdpHandler udpHandler = new UdpHandler(null, null, new DefaultMessageParser('|'), loggerMock);
        RuntimeException runtimeException = new RuntimeException("runtimeException message");
        udpHandler.exceptionCaught(context, runtimeException);

        Mockito.verify(loggerMock, Mockito.times(1)).error("Error handling Udp connection: runtimeException message", runtimeException);

    }

    private void verifyNoWrites(ChannelHandlerContext mockedContext) {
        Mockito.verify(mockedContext, Mockito.times(0)).writeAndFlush(Mockito.any());
        Mockito.verify(mockedContext, Mockito.times(0)).write(Mockito.any());
    }

    private void verifyNoWritesOrFlushes(ChannelHandlerContext mockedContext) {
        verifyNoWrites(mockedContext);
        Mockito.verify(mockedContext, Mockito.times(0)).flush();
    }

    private UdpHandler getHandler(ChannelHandlerContext ctx) {
        SessionStore<SockiopathSession> sessionStore = getSessionStore(ctx);
        Map<String, MessageBus> messageHandlers = getMessageHandlers();
        return new UdpHandler(sessionStore, messageHandlers);
    }

    private static Map<String, MessageBus> getMessageHandlers() {
        return Map.of(
                "address-a", new MessageBus((msg) -> CompletableFuture.completedFuture("response-a".getBytes()), 1000),
                "address-b", new MessageBus((msg) -> CompletableFuture.completedFuture("response-b".getBytes()), 1000),
                "address-timeout", new MessageBus((msg) -> new CompletableFuture<>(), 0)
        );
    }

    private static MapBackedSessionStore getSessionStore(ChannelHandlerContext ctx) {
        return new MapBackedSessionStore(Map.of(
                "sessionId-a", new SockiopathSession(ctx),
                "sessionId-noAddress", new SockiopathSession(ctx),
                "sessionId-timeout", new SockiopathSession(ctx)
        ));
    }
}