package io.worldy.sockiopath.websocket.session;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MapBackedSessionStore<T extends WebSocketSession> implements SessionStore<T> {
    private final Map<String, T> store;
    private final Function<String, T> get;
    private BiFunction<String, T, T> put;
    private Function<String, T> remove;
    private Supplier<Integer> size;
    private Supplier<Set<String>> keySet;

    public MapBackedSessionStore(Map<String, T> store) {
        this.store = Optional.ofNullable(store).orElseGet(Map::of);
        this.get = this.store::get;
        this.put = this.store::put;
        this.remove = this.store::remove;
        this.size = this.store::size;
        this.keySet = this.store::keySet;
    }

    @Override
    public Function<String, T> get() {
        return get;
    }

    @Override
    public BiFunction<String, T, T> put() {
        return put;
    }

    @Override
    public Function<String, T> remove() {
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
