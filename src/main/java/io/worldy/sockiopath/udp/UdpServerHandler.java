package io.worldy.sockiopath.udp;


import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.FutureListener;
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

    protected ChannelPool channelPool;

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
        if (channelPool != null) {
            DatagramPacket copy = datagramPacket.copy();
            Runnable channelRead = () ->
                    super.channelRead0(channelHandlerContext, datagramPacket.sender(), copy.content());
            channelPool.acquire().addListener((FutureListener<Channel>) f1 -> {
                if (f1.isSuccess()) {
                    Channel ch = f1.getNow();
                    channelRead.run();
                    channelPool.release(ch);
                } else {
                    logger.error("Error acquiring channel from pool. " + f1.cause().getMessage());
                }
            });
        } else {
            super.channelRead0(channelHandlerContext, datagramPacket.sender(), datagramPacket.content());
        }
    }

    public void setChannelPool(ChannelPool channelPool) {
        this.channelPool = channelPool;
    }
}
