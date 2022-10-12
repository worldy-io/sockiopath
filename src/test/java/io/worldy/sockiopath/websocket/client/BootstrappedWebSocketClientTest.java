package io.worldy.sockiopath.websocket.client;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BootstrappedWebSocketClientTest {
    @Test
    void uriSyntaxExceptionTest() {
        BootstrappedWebSocketClient bootstrappedWebSocketClient = new BootstrappedWebSocketClient(
                "   ",
                0,
                "/websocket",
                null,
                null,
                1000,
                1000
        );

        RuntimeException runtimeException = assertThrows(RuntimeException.class, bootstrappedWebSocketClient::startup);
        assertEquals(URISyntaxException.class, runtimeException.getCause().getClass());
    }
}
