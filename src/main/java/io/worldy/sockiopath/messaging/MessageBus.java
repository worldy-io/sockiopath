package io.worldy.sockiopath.messaging;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public record MessageBus(Function<SockiopathMessage, CompletableFuture<byte[]>> consumer, int timeoutMillis) {

}
