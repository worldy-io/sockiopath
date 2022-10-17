package io.worldy.sockiopath;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.worldy.sockiopath.messaging.MessageBus;
import io.worldy.sockiopath.session.MapBackedSessionStore;
import io.worldy.sockiopath.session.SessionStore;
import io.worldy.sockiopath.session.SockiopathSession;
import io.worldy.sockiopath.udp.UdpServerHandler;
import io.worldy.sockiopath.websocket.WebSocketServerHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNull;

public class SockiopathServerHandlerTest {

    @Test
    void channelRegistered() throws Exception {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        SockiopathServerHandler<Object> handler = getWebSocketHandler(getSessionStore(context));
        handler.channelRegistered(context);
        Mockito.verify(context, Mockito.times(1)).fireChannelRegistered();
        assertNull(handler.channelHandlerContext);
        verifyNoWrites(context);
    }

    public static void verifyNoWrites(ChannelHandlerContext mockedContext) {
        Mockito.verify(mockedContext, Mockito.times(0)).writeAndFlush(Mockito.any());
        Mockito.verify(mockedContext, Mockito.times(0)).write(Mockito.any());
    }

    public static void verifyNoWritesOrFlushes(ChannelHandlerContext mockedContext) {
        verifyNoWrites(mockedContext);
        Mockito.verify(mockedContext, Mockito.times(0)).flush();
    }

    public static SockiopathServerHandler<DatagramPacket> getUdpHandler(ChannelHandlerContext ctx) {
        SessionStore<SockiopathSession> sessionStore = getSessionStore(ctx);
        Map<String, MessageBus> messageHandlers = getMessageHandlers();
        return new UdpServerHandler(sessionStore, messageHandlers);
    }

    public static SockiopathServerHandler<Object> getWebSocketHandler(Map<String, SockiopathSession> sessions) {
        SessionStore<SockiopathSession> sessionStore = getSessionStore(sessions);
        return getWebSocketHandler(sessionStore);
    }

    public static SockiopathServerHandler<Object> getWebSocketHandler(SessionStore<SockiopathSession> sessionStore) {
        Map<String, MessageBus> messageHandlers = getMessageHandlers();
        return new WebSocketServerHandler(sessionStore, messageHandlers);
    }

    public static Map<String, MessageBus> getMessageHandlers() {
        return Map.of(
                "address-a", new MessageBus((msg) -> CompletableFuture.completedFuture("response-a".getBytes()), 1000),
                "address-b", new MessageBus((msg) -> CompletableFuture.completedFuture("response-b".getBytes()), 1000),
                "address-timeout", new MessageBus((msg) -> new CompletableFuture<>(), 0)
        );
    }

    public static MapBackedSessionStore getSessionStore(ChannelHandlerContext ctx) {
        return getSessionStore(Map.of(
                "sessionId-a", new SockiopathSession(ctx),
                "sessionId-noAddress", new SockiopathSession(ctx),
                "sessionId-timeout", new SockiopathSession(ctx)
        ));
    }

    public static MapBackedSessionStore getSessionStore(Map<String, SockiopathSession> sessions) {
        return new MapBackedSessionStore(sessions);
    }
}
