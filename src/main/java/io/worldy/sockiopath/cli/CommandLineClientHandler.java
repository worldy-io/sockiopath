package io.worldy.sockiopath.cli;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.worldy.sockiopath.udp.UdpServer;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandLineClientHandler extends SimpleChannelInboundHandler<Object> {

    private final AtomicInteger messageIndexer;
    private final Map<Integer, Object> responseMap;

    private String sessionId;

    public CommandLineClientHandler(AtomicInteger messageIndexer, Map<Integer, Object> responseMap) {
        this.messageIndexer = messageIndexer;
        this.responseMap = responseMap;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {


        if (msg instanceof TextWebSocketFrame frame) {
            String text = frame.text();

            responseMap.put(messageIndexer.incrementAndGet(), frame.copy());
        } else if (msg instanceof DatagramPacket datagramPacket) {
            responseMap.put(messageIndexer.incrementAndGet(), datagramPacket.copy());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
