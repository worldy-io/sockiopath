package io.worldy.sockiopath.websocket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.worldy.sockiopath.SockiopathHandler;
import io.worldy.sockiopath.SockiopathHandlerTest;
import io.worldy.sockiopath.messaging.DefaultMessageParser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.HashMap;

import static io.worldy.sockiopath.SockiopathHandlerTest.getMessageHandlers;
import static io.worldy.sockiopath.SockiopathHandlerTest.getSessionStore;
import static io.worldy.sockiopath.SockiopathHandlerTest.getWebSocketHandler;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebSocketHandlerTest {

    @Test
    void channelRead0BinaryTest() throws Exception {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(context.channel()).thenReturn(channel);
        BinaryWebSocketFrame packet = Mockito.mock(BinaryWebSocketFrame.class);

        SockiopathHandler<Object> sockiopathHandler = getWebSocketHandler(getSessionStore(context));

        ByteBuf content = Unpooled.wrappedBuffer("address-a|sessionId-a|data-a".getBytes());
        Mockito.when(packet.content()).thenReturn(content);

        sockiopathHandler.channelRead0(context, packet);
        Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.any());
        assertNull(sockiopathHandler.getSession("sessionId-a").getUdpSocketAddress());
        assertNull(sockiopathHandler.getSession("sessionId-a").getUdpContext());
        assertEquals(context, sockiopathHandler.getSession("sessionId-a").getWebSocketContext());
    }

    @Test
    void channelRead0BinaryCustomDeliminatorTest() throws Exception {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(context.channel()).thenReturn(channel);
        BinaryWebSocketFrame packet = Mockito.mock(BinaryWebSocketFrame.class);

        SockiopathHandler<Object> sockiopathHandler = new WebSocketHandler(getSessionStore(context), getMessageHandlers(), '|');

        ByteBuf content = Unpooled.wrappedBuffer("address-a|sessionId-a|data-a".getBytes());
        Mockito.when(packet.content()).thenReturn(content);

        sockiopathHandler.channelRead0(context, packet);
        Mockito.verify(channel, Mockito.times(1)).writeAndFlush(Mockito.any());
        assertNull(sockiopathHandler.getSession("sessionId-a").getUdpSocketAddress());
        assertNull(sockiopathHandler.getSession("sessionId-a").getUdpContext());
        assertEquals(context, sockiopathHandler.getSession("sessionId-a").getWebSocketContext());
    }

    @Test
    void channelRead0LoggingNoSessionTest() throws Exception {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(context.channel()).thenReturn(channel);
        BinaryWebSocketFrame packet = Mockito.mock(BinaryWebSocketFrame.class);

        Logger loggerMock = Mockito.mock(Logger.class);
        Mockito.when(loggerMock.isDebugEnabled()).thenReturn(true);

        SockiopathHandler<Object> sockiopathHandler =
                new WebSocketHandler(getSessionStore(context), getMessageHandlers(), new DefaultMessageParser('|'), loggerMock);

        ByteBuf content = Unpooled.wrappedBuffer("address-a|no-session|data-a".getBytes());
        Mockito.when(packet.content()).thenReturn(content);

        sockiopathHandler.channelRead0(context, packet);
        Mockito.verify(channel, Mockito.times(0)).writeAndFlush(Mockito.any());
        assertNull(sockiopathHandler.getSession("no-session"));
        Mockito.verify(loggerMock, Mockito.times(1)).debug("No session for request. sender: [VIRTUAL_INET_SOCKET_ADDRESS].");
    }

    @Test
    void channelRead0TextFrameJoinSessionTest() throws Exception {

        SockiopathHandler<Object> sockiopathHandler = getWebSocketHandler(new HashMap<>());

        ChannelHandlerContext ctx1 = mockContext(1);
        sockiopathHandler.channelRead0(ctx1, new TextWebSocketFrame("join"));
        assertEquals(1, sockiopathHandler.getSessionCount());
        sockiopathHandler.channelRead0(ctx1, new TextWebSocketFrame("test1-1"));

        ChannelHandlerContext sessionContext1 = sockiopathHandler.getSession("long1").getWebSocketContext();
        assertEquals(ctx1, sessionContext1);
        Mockito.verify(sessionContext1, Mockito.times(1)).writeAndFlush(Mockito.any());


        ChannelHandlerContext ctx2 = mockContext(2);
        sockiopathHandler.channelRead0(ctx2, new TextWebSocketFrame("join"));
        assertEquals(2, sockiopathHandler.getSessionCount());
        sockiopathHandler.channelRead0(ctx2, new TextWebSocketFrame("test2-1"));

        ChannelHandlerContext sessionContext2 = sockiopathHandler.getSession("long2").getWebSocketContext();
        assertEquals(ctx2, sessionContext2);
        Mockito.verify(sessionContext2, Mockito.times(1)).writeAndFlush(Mockito.any());
        Mockito.verify(sessionContext1, Mockito.times(2)).writeAndFlush(Mockito.any());

        sockiopathHandler.channelUnregistered(ctx1);

        sockiopathHandler.channelRead0(ctx2, new TextWebSocketFrame("test2-2"));
        assertEquals(1, sockiopathHandler.getSessionCount());
        Mockito.verify(sessionContext2, Mockito.times(2)).writeAndFlush(Mockito.any());
        Mockito.verify(sessionContext1, Mockito.times(2)).writeAndFlush(Mockito.any());


        sockiopathHandler.channelRead0(ctx1, new TextWebSocketFrame("test1-2"));
        Mockito.verify(sessionContext2, Mockito.times(2)).writeAndFlush(Mockito.any());
        Mockito.verify(sessionContext1, Mockito.times(2)).writeAndFlush(Mockito.any());

        Exception exception = assertThrows(UnsupportedOperationException.class, () -> sockiopathHandler.channelRead0(ctx2, "test1-3"));
        assertEquals("unsupported frame type: java.lang.String", exception.getMessage());
    }

    @Test
    void exceptionCaughtTest() {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        Logger loggerMock = Mockito.mock(Logger.class);
        SockiopathHandler<Object> sockioPathHandler = new WebSocketHandler(null, null, new DefaultMessageParser('|'), loggerMock);
        RuntimeException runtimeException = new RuntimeException("runtimeException message");
        sockioPathHandler.exceptionCaught(context, runtimeException);

        Mockito.verify(loggerMock, Mockito.times(1)).error("Error handling connection: runtimeException message", runtimeException);
        Mockito.verify(context, Mockito.never()).close();
    }

    @Test
    void getChannelHandlerContextTest() {
        SockiopathHandler<Object> sockiopathHandler = new WebSocketHandler(null, null);
        Exception ex = assertThrows(UnsupportedOperationException.class, sockiopathHandler::getChannelHandlerContext);
        assertEquals("Getting the ChannelHandlerContext is only supported for UdpHandlers.", ex.getMessage());
    }

    private ChannelHandlerContext mockContext(int sessionId) {
        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        ChannelId channelId = Mockito.mock(ChannelId.class);
        Mockito.when(channelId.asLongText()).thenReturn("long" + sessionId);
        Mockito.when(channelId.asShortText()).thenReturn("short" + sessionId);
        Mockito.when(channel.id()).thenReturn(channelId);
        Mockito.when(ctx.channel()).thenReturn(channel);
        return ctx;
    }
}