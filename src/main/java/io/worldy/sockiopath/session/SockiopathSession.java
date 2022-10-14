package io.worldy.sockiopath.session;

import io.netty.channel.ChannelHandlerContext;

public class SockiopathSession {
    private final ChannelHandlerContext context;

    public SockiopathSession(ChannelHandlerContext context) {
        this.context = context;
    }

    public ChannelHandlerContext getContext() {
        return context;
    }
}
