package io.worldy.sockiopath;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.pool.ChannelPool;
import io.worldy.sockiopath.messaging.MessageBus;
import io.worldy.sockiopath.messaging.SockiopathMessage;
import io.worldy.sockiopath.session.SessionStore;
import io.worldy.sockiopath.session.SockiopathSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class SockiopathServerHandler<T> extends SockiopathHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SockiopathServerHandler.class);

    protected SessionStore<SockiopathSession> sessionStore;

    ChannelHandlerContext channelHandlerContext;

    public SockiopathServerHandler(
            SessionStore<SockiopathSession> sessionStore,
            Map<String, MessageBus> messageHandlers,
            Function<ByteBuffer, Optional<SockiopathMessage>> messageParser,
            Logger logger
    ) {
        super(messageHandlers, messageParser, logger);
        this.sessionStore = sessionStore;
    }

    public void setChannelPool(ChannelPool channelPool) {
        throw new UnsupportedOperationException();
    }

    protected void process(SockiopathMessage sockiopathMessage, ChannelHandlerContext context, InetSocketAddress sender) {

        SockiopathSession session = sessionStore.get().apply(sockiopathMessage.sessionId());
        if (session == null) {
            Optional<InetAddress> address = Optional.ofNullable(sender.getAddress());
            logger.debug(
                    NO_SESSION_ERROR_MESSAGE.formatted(address.map(InetAddress::getCanonicalHostName).orElse(sender.getHostName()))
            );
            return;
        }
        if (isUdp()) {
            session.withUdpSocketAddress(sender);
            session.withUdpContext(context);
        }

        super.process(sockiopathMessage, context, sender);

    }

    public SockiopathSession getSession(String id) {
        return sessionStore.get().apply(id);
    }

    public int getSessionCount() {
        return sessionStore.size().get();
    }
}
