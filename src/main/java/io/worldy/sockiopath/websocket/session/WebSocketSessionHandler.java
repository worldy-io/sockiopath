package io.worldy.sockiopath.websocket.session;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketSessionHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionHandler.class);
    private static final String TEXT_COMMAND_JOIN = "join";

    private static final String DELIMINATOR = "|";
    private static final String TEXT_RESPONSE_PART_SESSION = "session" + DELIMINATOR;
    private final SessionStore sessionStore;

    public WebSocketSessionHandler(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object frame) {

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
                    sessionStore.get().apply(key).getContext().writeAndFlush(new TextWebSocketFrame(prefix + textMessage));
                });
            }
        } else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }

    private void createSession(ChannelHandlerContext ctx) {
        String sessionId = getChannelId(ctx.channel());
        logger.debug("createSession {}", sessionId);
        sessionStore.put().apply(sessionId, new WebSocketSession(ctx));
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
