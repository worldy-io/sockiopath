package io.worldy.sockiopath.udp.client;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.worldy.sockiopath.SockiopathHandler;
import io.worldy.sockiopath.SockiopathServerHandler;
import io.worldy.sockiopath.messaging.MessageBus;
import io.worldy.sockiopath.messaging.SockiopathMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class UdpClientHandler extends SockiopathHandler<DatagramPacket> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SockiopathServerHandler.class);

    public UdpClientHandler(
            Map<String, MessageBus> messageHandlers,
            Function<ByteBuffer, Optional<SockiopathMessage>> messageParser,
            Logger logger
    ) {
        super(messageHandlers, messageParser, logger);
    }

    public UdpClientHandler(
            Map<String, MessageBus> messageHandlers,
            char deliminator

    ) {
        this(messageHandlers, getDefaultMessageParser(deliminator), LOGGER);
    }

    public UdpClientHandler(
            Map<String, MessageBus> messageHandlers
    ) {
        this(messageHandlers, DEFAULT_MESSAGE_DELIMINATOR);
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
