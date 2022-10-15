package io.worldy.sockiopath;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.worldy.sockiopath.messaging.MessageBus;
import io.worldy.sockiopath.session.MapBackedSessionStore;
import io.worldy.sockiopath.session.SessionStore;
import io.worldy.sockiopath.session.SockiopathSession;
import io.worldy.sockiopath.udp.UdpHandler;
import io.worldy.sockiopath.websocket.WebSocketHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNull;

public class SockiopathHandlerTest {

    @Test
    void channelRegistered() throws Exception {
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        SockiopathHandler<Object> handler = getWebSocketHandler(getSessionStore(context));
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

    public static SockiopathHandler<DatagramPacket> getUdpHandler(ChannelHandlerContext ctx) {
        SessionStore<SockiopathSession> sessionStore = getSessionStore(ctx);
        Map<String, MessageBus> messageHandlers = getMessageHandlers();
        return new UdpHandler(sessionStore, messageHandlers);
    }

    public static SockiopathHandler<Object> getWebSocketHandler(Map<String, SockiopathSession> sessions) {
        SessionStore<SockiopathSession> sessionStore = getSessionStore(sessions);
        return getWebSocketHandler(sessionStore);
    }

    public static SockiopathHandler<Object> getWebSocketHandler(SessionStore<SockiopathSession> sessionStore) {
        Map<String, MessageBus> messageHandlers = getMessageHandlers();
        return new WebSocketHandler(sessionStore, messageHandlers);
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
