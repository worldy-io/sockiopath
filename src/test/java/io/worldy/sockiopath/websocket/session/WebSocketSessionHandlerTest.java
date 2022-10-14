package io.worldy.sockiopath.websocket.session;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebSocketSessionHandlerTest {

    @Test
    void channelRead0() throws Exception {

        MapBackedSessionStore sessionStore = new MapBackedSessionStore(new HashMap<>());
        WebSocketSessionHandler webSocketSessionHandler = new WebSocketSessionHandler(sessionStore);

        ChannelHandlerContext ctx1 = mockContext(1);
        webSocketSessionHandler.channelRead0(ctx1, new TextWebSocketFrame("join"));
        assertEquals(1, sessionStore.size().get());
        webSocketSessionHandler.channelRead0(ctx1, new TextWebSocketFrame("test1-1"));

        ChannelHandlerContext sessionContext1 = sessionStore.get().apply("long1").getContext();
        assertEquals(ctx1, sessionContext1);
        Mockito.verify(sessionContext1, Mockito.times(1)).writeAndFlush(Mockito.any());


        ChannelHandlerContext ctx2 = mockContext(2);
        webSocketSessionHandler.channelRead0(ctx2, new TextWebSocketFrame("join"));
        assertEquals(2, sessionStore.size().get());
        webSocketSessionHandler.channelRead0(ctx2, new TextWebSocketFrame("test2-1"));

        ChannelHandlerContext sessionContext2 = sessionStore.get().apply("long2").getContext();
        assertEquals(ctx2, sessionContext2);
        Mockito.verify(sessionContext2, Mockito.times(1)).writeAndFlush(Mockito.any());
        Mockito.verify(sessionContext1, Mockito.times(2)).writeAndFlush(Mockito.any());

        webSocketSessionHandler.channelUnregistered(ctx1);

        webSocketSessionHandler.channelRead0(ctx2, new TextWebSocketFrame("test2-2"));
        assertEquals(1, sessionStore.size().get());
        Mockito.verify(sessionContext2, Mockito.times(2)).writeAndFlush(Mockito.any());
        Mockito.verify(sessionContext1, Mockito.times(2)).writeAndFlush(Mockito.any());


        webSocketSessionHandler.channelRead0(ctx1, new TextWebSocketFrame("test1-2"));
        Mockito.verify(sessionContext2, Mockito.times(2)).writeAndFlush(Mockito.any());
        Mockito.verify(sessionContext1, Mockito.times(2)).writeAndFlush(Mockito.any());

        Exception exception = assertThrows(UnsupportedOperationException.class, () -> webSocketSessionHandler.channelRead0(ctx2, "test1-3"));
        assertEquals("unsupported frame type: java.lang.String", exception.getMessage());
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