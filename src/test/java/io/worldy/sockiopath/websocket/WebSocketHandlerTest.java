package io.worldy.sockiopath.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.worldy.sockiopath.session.MapBackedSessionStore;
import io.worldy.sockiopath.session.SockiopathSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebSocketHandlerTest {

    @Test
    void channelRead0Test() throws Exception {

        WebSocketHandler webSocketHandler = new WebSocketHandler(new HashMap<>());

        testChannelRead0(webSocketHandler);
    }

    @Test
    void channelRead0TestWithCustomSessionStore() throws Exception {

        WebSocketHandler webSocketHandler = new WebSocketHandler(
                new MapBackedSessionStore(new HashMap<>()) {
                    @Override
                    public SockiopathSession createSession(ChannelHandlerContext ctx) {
                        return new SockiopathSession(ctx);
                    }
                }
        );

        testChannelRead0(webSocketHandler);
    }
    void testChannelRead0(WebSocketHandler webSocketHandler) throws Exception {

        ChannelHandlerContext ctx1 = mockContext(1);
        webSocketHandler.channelRead0(ctx1, new TextWebSocketFrame("join"));
        assertEquals(1, webSocketHandler.sessionStore.size().get());
        webSocketHandler.channelRead0(ctx1, new TextWebSocketFrame("test1-1"));

        ChannelHandlerContext sessionContext1 = webSocketHandler.sessionStore.get().apply("long1").getContext();
        assertEquals(ctx1, sessionContext1);
        Mockito.verify(sessionContext1, Mockito.times(1)).writeAndFlush(Mockito.any());


        ChannelHandlerContext ctx2 = mockContext(2);
        webSocketHandler.channelRead0(ctx2, new TextWebSocketFrame("join"));
        assertEquals(2, webSocketHandler.sessionStore.size().get());
        webSocketHandler.channelRead0(ctx2, new TextWebSocketFrame("test2-1"));

        ChannelHandlerContext sessionContext2 = webSocketHandler.sessionStore.get().apply("long2").getContext();
        assertEquals(ctx2, sessionContext2);
        Mockito.verify(sessionContext2, Mockito.times(1)).writeAndFlush(Mockito.any());
        Mockito.verify(sessionContext1, Mockito.times(2)).writeAndFlush(Mockito.any());

        webSocketHandler.channelUnregistered(ctx1);

        webSocketHandler.channelRead0(ctx2, new TextWebSocketFrame("test2-2"));
        assertEquals(1, webSocketHandler.sessionStore.size().get());
        Mockito.verify(sessionContext2, Mockito.times(2)).writeAndFlush(Mockito.any());
        Mockito.verify(sessionContext1, Mockito.times(2)).writeAndFlush(Mockito.any());


        webSocketHandler.channelRead0(ctx1, new TextWebSocketFrame("test1-2"));
        Mockito.verify(sessionContext2, Mockito.times(2)).writeAndFlush(Mockito.any());
        Mockito.verify(sessionContext1, Mockito.times(2)).writeAndFlush(Mockito.any());

        Exception exception = assertThrows(UnsupportedOperationException.class, () -> webSocketHandler.channelRead0(ctx2, "test1-3"));
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