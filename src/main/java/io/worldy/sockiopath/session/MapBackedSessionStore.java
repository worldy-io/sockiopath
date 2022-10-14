package io.worldy.sockiopath.session;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MapBackedSessionStore implements SessionStore<SockiopathSession> {
    private final Map<String, SockiopathSession> store;
    private final Function<String, SockiopathSession> get;
    private final BiFunction<String, SockiopathSession, SockiopathSession> put;
    private final Function<String, SockiopathSession> remove;
    private final Supplier<Integer> size;
    private final Supplier<Set<String>> keySet;

    public MapBackedSessionStore(Map<String, SockiopathSession> store) {
        this.store = Optional.ofNullable(store).orElseGet(Map::of);
        this.get = this.store::get;
        this.put = this.store::put;
        this.remove = this.store::remove;
        this.size = this.store::size;
        this.keySet = this.store::keySet;
    }


    @Override
    public Function<String, SockiopathSession> get() {
        return get;
    }

    @Override
    public BiFunction<String, SockiopathSession, SockiopathSession> put() {
        return put;
    }

    @Override
    public Function<String, SockiopathSession> remove() {
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
