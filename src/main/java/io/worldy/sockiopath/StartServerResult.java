package io.worldy.sockiopath;

import io.netty.channel.ChannelFuture;

public record StartServerResult(Integer port, ChannelFuture closeFuture) {
}
