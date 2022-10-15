package io.worldy.sockiopath.session;

import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;

public class SockiopathSession {
    private final ChannelHandlerContext webSocketContext;
    private InetSocketAddress udpSocketAddress;

    private ChannelHandlerContext udpContext;

    public SockiopathSession(ChannelHandlerContext webSocketContext) {
        this.webSocketContext = webSocketContext;
    }

    public ChannelHandlerContext getWebSocketContext() {
        return webSocketContext;
    }

    public InetSocketAddress getUdpSocketAddress() {
        return udpSocketAddress;
    }

    public SockiopathSession withUdpSocketAddress(InetSocketAddress udpSocketAddress) {
        this.udpSocketAddress = udpSocketAddress;
        return this;
    }

    public ChannelHandlerContext getUdpContext() {
        return udpContext;
    }

    public SockiopathSession withUdpContext(ChannelHandlerContext udpContext) {
        this.udpContext = udpContext;
        return this;
    }
}
