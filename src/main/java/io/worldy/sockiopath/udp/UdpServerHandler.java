package io.worldy.sockiopath.udp;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.worldy.sockiopath.SockiopathServerHandler;
import io.worldy.sockiopath.messaging.MessageBus;
import io.worldy.sockiopath.messaging.SockiopathMessage;
import io.worldy.sockiopath.session.SessionStore;
import io.worldy.sockiopath.session.SockiopathSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class UdpServerHandler extends SockiopathServerHandler<DatagramPacket> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SockiopathServerHandler.class);

    public UdpServerHandler(
            SessionStore<SockiopathSession> sessionStore,
            Map<String, MessageBus> messageHandlers,
            Function<ByteBuffer, Optional<SockiopathMessage>> messageParser,
            Logger logger
    ) {
        super(sessionStore, messageHandlers, messageParser, logger);
    }

    public UdpServerHandler(
            SessionStore<SockiopathSession> sessionStore,
            Map<String, MessageBus> messageHandlers,
            char deliminator
    ) {
        this(sessionStore, messageHandlers, getDefaultMessageParser(deliminator), LOGGER);
    }

    public UdpServerHandler(
            SessionStore<SockiopathSession> sessionStore,
            Map<String, MessageBus> messageHandlers
    ) {
        this(sessionStore, messageHandlers, DEFAULT_MESSAGE_DELIMINATOR);
    }

    @Override
    protected boolean isUdp() {
        return true;
    }


    @Override
    public void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
        super.channelRead0(channelHandlerContext, datagramPacket.sender(), datagramPacket.content());
    }
}
