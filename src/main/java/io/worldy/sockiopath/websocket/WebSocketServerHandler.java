package io.worldy.sockiopath.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.worldy.sockiopath.SockiopathServerHandler;
import io.worldy.sockiopath.messaging.MessageBus;
import io.worldy.sockiopath.messaging.SockiopathMessage;
import io.worldy.sockiopath.session.SessionStore;
import io.worldy.sockiopath.session.SockiopathSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class WebSocketServerHandler extends SockiopathServerHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServerHandler.class);
    private static final String TEXT_COMMAND_JOIN = "join";

    private static final String DELIMINATOR = "|";
    private static final String TEXT_RESPONSE_PART_SESSION = "session" + DELIMINATOR;

    private static final InetSocketAddress VIRTUAL_INET_SOCKET_ADDRESS = new InetSocketAddress("VIRTUAL_INET_SOCKET_ADDRESS", 42);


    public WebSocketServerHandler(
            SessionStore<SockiopathSession> sessionStore,
            Map<String, MessageBus> messageHandlers,
            Function<ByteBuffer, Optional<SockiopathMessage>> messageParser,
            Logger logger
    ) {
        super(sessionStore, messageHandlers, messageParser, logger);
    }

    public WebSocketServerHandler(
            SessionStore<SockiopathSession> sessionStore,
            Map<String, MessageBus> messageHandlers,
            char deliminator
    ) {
        super(sessionStore, messageHandlers, deliminator);
    }

    public WebSocketServerHandler(
            SessionStore<SockiopathSession> sessionStore,
            Map<String, MessageBus> messageHandlers
    ) {
        super(sessionStore, messageHandlers);
    }

    @Override
    protected boolean isUdp() {
        return false;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object frame) {

        if (frame instanceof TextWebSocketFrame textFrame) {
            String textMessage = textFrame.text();
            logger.debug("{} received {}", ctx.channel(), textMessage);
            logger.debug("sessions {}", sessionStore.size());

            String sessionId = getChannelId(ctx.channel());
            String sessionShortId = getChannelShortId(ctx.channel());
            if (TEXT_COMMAND_JOIN.equals(textMessage)) {
                createSession(ctx);
                ctx.channel().writeAndFlush(new TextWebSocketFrame(TEXT_RESPONSE_PART_SESSION + sessionId));
            } else if (sessionStore.get().apply(sessionId) == null) {
                logger.debug("message with no session: " + textMessage);
            } else {
                sessionStore.keySet().get().forEach((key) -> {
                    boolean isSameSession = key.equals(sessionId);
                    String prefix = isSameSession ? "" : (sessionShortId + ": ");
                    sessionStore.get().apply(key).getWebSocketContext().writeAndFlush(new TextWebSocketFrame(prefix + textMessage));
                });
            }
        } else if (frame instanceof BinaryWebSocketFrame binaryFrame) {
            super.channelRead0(ctx, VIRTUAL_INET_SOCKET_ADDRESS, binaryFrame.content());
        } else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }

    private void createSession(ChannelHandlerContext ctx) {
        String sessionId = getChannelId(ctx.channel());
        logger.debug("createSession {}", sessionId);
        sessionStore.put().apply(sessionId, sessionStore.createSession(ctx));
    }

    private void removeSession(ChannelHandlerContext ctx) {
        String sessionId = getChannelId(ctx.channel());
        logger.debug("removeSession {}", sessionId);
        synchronized (sessionStore) {
            sessionStore.remove().apply(sessionId);
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        removeSession(ctx);
    }

    private String getChannelId(Channel channel) {
        return channel.id().asLongText();
    }

    private String getChannelShortId(Channel channel) {
        return channel.id().asShortText();
    }

}
