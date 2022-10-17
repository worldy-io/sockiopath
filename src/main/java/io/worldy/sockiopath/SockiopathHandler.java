package io.worldy.sockiopath;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.worldy.sockiopath.messaging.DefaultMessageParser;
import io.worldy.sockiopath.messaging.MessageBus;
import io.worldy.sockiopath.messaging.SockiopathMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public abstract class SockiopathHandler<T> extends SimpleChannelInboundHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SockiopathHandler.class);

    protected static final char DEFAULT_MESSAGE_DELIMINATOR = '|';
    protected static final String PARSE_MESSAGE_ERROR_MESSAGE = "Unable to parse message. sender: [%s]. content: ";
    protected static final String ENABLE_DEBUGGING_MESSAGE = "Enable debugging to see content";
    protected static final String NO_SESSION_ERROR_MESSAGE = "No session for request. sender: [%s].";
    protected static final String HANDLE_MESSAGE_DEBUG_MESSAGE = "Handling message. address: [%s]. content: ";

    protected final Function<ByteBuffer, Optional<SockiopathMessage>> messageParser;

    protected final Map<String, MessageBus> messageHandlers;

    protected final Logger logger;

    ChannelHandlerContext channelHandlerContext;

    public SockiopathHandler(
            Map<String, MessageBus> messageHandlers,
            Function<ByteBuffer, Optional<SockiopathMessage>> messageParser,
            Logger logger
    ) {
        this.messageParser = messageParser;
        this.messageHandlers = messageHandlers;
        this.logger = logger;
    }

    public void channelRead0(ChannelHandlerContext context, InetSocketAddress sender, ByteBuf payload) {
        messageParser.apply(payload.copy().nioBuffer())
                .ifPresentOrElse(
                        sockiopathMessage -> process(sockiopathMessage, context, sender),
                        () -> logError(sender, payload.copy().nioBuffer())
                );
    }

    protected void logError(InetSocketAddress sender, ByteBuffer content) {
        final String message;
        if (logger.isDebugEnabled()) {
            message = SockiopathServer.byteBufferToString(content);
        } else {
            message = ENABLE_DEBUGGING_MESSAGE;
        }
        logger.error(PARSE_MESSAGE_ERROR_MESSAGE.formatted(sender.getAddress().getCanonicalHostName()) + message);
    }

    protected void process(SockiopathMessage sockiopathMessage, ChannelHandlerContext context, InetSocketAddress sender) {

        Optional.ofNullable(messageHandlers.get(sockiopathMessage.address()))
                .ifPresentOrElse(
                        handler -> {
                            if (logger.isDebugEnabled()) {
                                logger.debug(HANDLE_MESSAGE_DEBUG_MESSAGE.formatted(sockiopathMessage.address()) + new String(sockiopathMessage.data()));
                            }

                            handler.consumer().apply(sockiopathMessage)
                                    .orTimeout(handler.timeoutMillis(), TimeUnit.MILLISECONDS)
                                    .whenComplete((response, error) -> {
                                        if (error == null) {
                                            if (isUdp()) {
                                                context.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(response), sender));
                                            } else {
                                                context.channel().writeAndFlush(new BinaryWebSocketFrame(Unpooled.copiedBuffer(response)));
                                            }
                                        } else {
                                            logger.error(error.getMessage(), error);
                                        }
                                    });
                        },
                        () -> logger.debug("No message handler for: " + sockiopathMessage.address())
                );

    }

    protected abstract boolean isUdp();

    public abstract void channelRead0(ChannelHandlerContext context, T var2) throws Exception;

    @Override
    public void channelReadComplete(ChannelHandlerContext context) {
        context.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        //TODO capture failures in the session to mark them for removal via circuit breaker pattern
        logger.error("Error handling connection: " + cause.getMessage(), cause);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        if (isUdp()) {
            this.channelHandlerContext = ctx;
        }
    }

    public ChannelHandlerContext getChannelHandlerContext() {
        if (isUdp()) {
            return channelHandlerContext;
        }
        throw new UnsupportedOperationException("Getting the ChannelHandlerContext is only supported for UdpHandlers.");
    }

    protected static DefaultMessageParser getDefaultMessageParser(char deliminator) {
        return new DefaultMessageParser(deliminator);
    }
}
