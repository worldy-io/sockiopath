package io.worldy.sockiopath.websocket.session;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MapBackedSessionStore implements SessionStore {
    private final Map<String, WebSocketSession> store;
    private final Function<String, WebSocketSession> get;
    private BiFunction<String, WebSocketSession, WebSocketSession> put;
    private Function<String, WebSocketSession> remove;
    private Supplier<Integer> size;
    private Supplier<Set<String>> keySet;

    public MapBackedSessionStore(Map<String, WebSocketSession> store) {
        this.store = Optional.ofNullable(store).orElseGet(Map::of);
        this.get = this.store::get;
        this.put = this.store::put;
        this.remove = this.store::remove;
        this.size = this.store::size;
        this.keySet = this.store::keySet;
    }

    @Override
    public Function<String, WebSocketSession> get() {
        return get;
    }

    @Override
    public BiFunction<String, WebSocketSession, WebSocketSession> put() {
        return put;
    }

    @Override
    public Function<String, WebSocketSession> remove() {
        return remove;
    }


    @Override
    public Supplier<Integer> size() {
        return this.size;
    }

    @Override
    public Supplier<Set<String>> keySet() {
        return this.keySet;
    }

}
