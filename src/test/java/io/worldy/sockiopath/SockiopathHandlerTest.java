package io.worldy.sockiopath;

import io.worldy.sockiopath.messaging.MessageBus;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SockiopathHandlerTest {
    public static Map<String, MessageBus> getMessageHandlers() {
        return Map.of(
                "address-a", new MessageBus((msg) -> CompletableFuture.completedFuture("response-a".getBytes()), 1000),
                "address-b", new MessageBus((msg) -> CompletableFuture.completedFuture("response-b".getBytes()), 1000),
                "address-timeout", new MessageBus((msg) -> new CompletableFuture<>(), 0),
                "address-empty", new MessageBus((msg) -> CompletableFuture.completedFuture(null), 1000)
        );
    }
}
