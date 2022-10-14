package io.worldy.sockiopath.websocket.session;

import io.netty.channel.ChannelHandlerContext;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface SessionStore<T extends WebSocketSession> {
    Function<String, T> get();

    BiFunction<String, T, T> put();

    Function<String, T> remove();

    Supplier<Integer> size();

    Supplier<Set<String>> keySet();

    T createSession(ChannelHandlerContext ctx);
}
