package io.worldy.sockiopath;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;

import java.util.concurrent.ExecutorService;

public abstract class AbstractSockiopathServer implements SockiopathServer {

    protected final ChannelHandler channelHandler;

    protected final ExecutorService executorService;

    protected ChannelFuture closeFuture;
    protected final int port;
    protected int actualPort;

    public AbstractSockiopathServer(
            ChannelHandler channelHandler,
            ExecutorService executorService,
            int port
    ) {
        this.channelHandler = channelHandler;
        this.executorService = executorService;
        this.port = port;
    }


    @Override
    public int actualPort() {
        return actualPort;
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }

    @Override
    public ChannelFuture getCloseFuture() {
        return closeFuture;
    }
}
