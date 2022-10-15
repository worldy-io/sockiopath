package io.worldy.sockiopath.udp;


import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.worldy.sockiopath.messaging.DefaultMessageParser;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class UdpHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UdpHandler.class);

    public static final char DEFAULT_MESSAGE_DELIMINATOR = '|';
    private static final String PARSE_MESSAGE_ERROR_MESSAGE = "Unable to parse UDP message. sender: [%s]. content: ";
    private static final String ENABLE_DEBUGGING_MESSAGE = "Enable debugging to see content";
    private static final String NO_SESSION_ERROR_MESSAGE = "No session for request. sender: [%s].";
    private static final String HANDLE_MESSAGE_DEBUG_MESSAGE = "Handling message. address: [%s]. content: ";

    SessionStore<SockiopathSession> sessionStore;

    private final Function<ByteBuffer, Optional<SockiopathMessage>> messageParser;

    private final Map<String, MessageBus> messageHandlers;

    private final Logger logger;

    private ChannelHandlerContext channelHandlerContext;

    public UdpHandler(
            SessionStore<SockiopathSession> sessionStore,
            Map<String, MessageBus> messageHandlers,
            Function<ByteBuffer, Optional<SockiopathMessage>> messageParser,
            Logger logger
    ) {
        this.sessionStore = sessionStore;
        this.messageParser = messageParser;
        this.messageHandlers = messageHandlers;
        this.logger = logger;
    }

    public UdpHandler(
            SessionStore<SockiopathSession> sessionStore,
            Map<String, MessageBus> messageHandlers,
            char deliminator
    ) {
        this(sessionStore, messageHandlers, new DefaultMessageParser(deliminator), LOGGER);
    }

    public UdpHandler(
            SessionStore<SockiopathSession> sessionStore,
            Map<String, MessageBus> messageHandlers
    ) {
        this(sessionStore, messageHandlers, DEFAULT_MESSAGE_DELIMINATOR);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channelHandlerContext = ctx;
    }

    @Override
    public void channelRead0(ChannelHandlerContext context, DatagramPacket packet) {
        messageParser.apply(packet.content().copy().nioBuffer())
                .ifPresentOrElse(
                        sockiopathMessage -> process(sockiopathMessage, context, packet),
                        () -> logError(packet, packet.content().nioBuffer())
                );
    }

    protected void logError(DatagramPacket packet, ByteBuffer content) {
        final String message;
        if (logger.isDebugEnabled()) {
            message = UdpServer.byteBufferToString(content);
        } else {
            message = ENABLE_DEBUGGING_MESSAGE;
        }
        logger.error(PARSE_MESSAGE_ERROR_MESSAGE.formatted(packet.sender().getAddress().getCanonicalHostName()) + message);
    }

    protected void process(SockiopathMessage sockiopathMessage, ChannelHandlerContext context, DatagramPacket packet) {
        InetSocketAddress sender = packet.sender();

        SockiopathSession session = sessionStore.get().apply(sockiopathMessage.sessionId());
        if (session == null) {
            logger.debug(NO_SESSION_ERROR_MESSAGE.formatted(sender.getAddress().getCanonicalHostName()));
            return;
        }
        session.withUdpSocketAddress(packet.sender());
        session.withUdpContext(context);

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
                                            context.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(response), sender));
                                        } else {
                                            logger.error(error.getMessage(), error);
                                        }
                                    });
                        },
                        () -> logger.debug("No message handler for: " + sockiopathMessage.address())
                );

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext context) {
        context.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Error handling Udp connection: " + cause.getMessage(), cause);
        // We don't close the channel because we can keep serving requests.
    }

    public ChannelHandlerContext getChannelHandlerContext() {
        return channelHandlerContext;
    }
}
