package io.worldy.sockiopath.websocket.session;

import io.netty.channel.ChannelHandlerContext;

public class WebSocketSession {
    private final ChannelHandlerContext context;

    public WebSocketSession(ChannelHandlerContext context) {
        this.context = context;
    }

    public ChannelHandlerContext getContext() {
        return context;
    }
}
