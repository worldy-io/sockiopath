package io.worldy.sockiopath.messaging;

public record SockiopathMessage(
        String address,
        String sessionId,
        byte[] data
) {
}
