package io.worldy.sockiopath.websocket.session;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface SessionStore {
    Function<String, WebSocketSession> get();

    BiFunction<String, WebSocketSession, WebSocketSession> put();

    Function<String, WebSocketSession> remove();

    Supplier<Integer> size();

    Supplier<Set<String>> keySet();
}
