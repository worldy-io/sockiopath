package io.worldy.sockiopath;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class CountDownLatchChannelHandler extends SimpleChannelInboundHandler<Object> {

    private final CountDownLatch latch;
    private final Map<Long, Object> responseMap;
    private final Consumer<String> debug;

    public CountDownLatchChannelHandler(CountDownLatch latch, Map<Long, Object> responseMap, Consumer<String> debug) {
        this.latch = latch;
        this.responseMap = responseMap;
        this.debug = debug;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof TextWebSocketFrame frame) {
            debug.accept(frame.copy().text());
            responseMap.put(latch.getCount(), frame.copy());
            latch.countDown();
        } else if (msg instanceof DatagramPacket datagramPacket) {
            debug.accept(SockiopathServer.byteBufferToString(datagramPacket.copy().content().nioBuffer()));
            responseMap.put(latch.getCount(), datagramPacket.copy());
            latch.countDown();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
