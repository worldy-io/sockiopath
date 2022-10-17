package io.worldy.sockiopath.websocket.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.worldy.sockiopath.SockiopathHandler;
import io.worldy.sockiopath.messaging.MessageBus;
import io.worldy.sockiopath.messaging.SockiopathMessage;
import io.worldy.sockiopath.websocket.WebSocketServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class WebSocketClientHandler extends SockiopathHandler<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketClientHandler.class);

    public WebSocketClientHandler(
            Map<String, MessageBus> messageHandlers,
            Function<ByteBuffer, Optional<SockiopathMessage>> messageParser,
            Logger logger
    ) {
        super(messageHandlers, messageParser, logger);
    }

    public WebSocketClientHandler(
            Map<String, MessageBus> messageHandlers,
            char deliminator
    ) {
        this(messageHandlers, getDefaultMessageParser(deliminator), LOGGER);
    }

    public WebSocketClientHandler(
            Map<String, MessageBus> messageHandlers
    ) {
        this(messageHandlers, DEFAULT_MESSAGE_DELIMINATOR);
    }

    @Override
    protected boolean isUdp() {
        return false;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object frame) {
        if (frame instanceof TextWebSocketFrame textFrame) {
            logger.debug("MESSAGE received: " + textFrame.text());
        } else if (frame instanceof BinaryWebSocketFrame binaryFrame) {
            logger.debug("BINARY received");
            super.channelRead0(ctx, WebSocketServerHandler.VIRTUAL_INET_SOCKET_ADDRESS, binaryFrame.content());
        } else {
            logger.error("Unsupported frame type: " + frame.getClass().getName());
        }
    }

}
