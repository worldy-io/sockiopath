package io.worldy.sockiopath.udp.client;

import io.worldy.sockiopath.CountDownLatchChannelHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BootstrappedUdpClientTest {

    @Test
    void startup() {
        BootstrappedUdpClient client = new BootstrappedUdpClient(
                "localhost",
                0,
                new CountDownLatchChannelHandler(null, null, (message) -> {
                }),
                0
        );
        RuntimeException startTimeoutException = assertThrows(RuntimeException.class, client::startup);
        assertEquals("Client took too long to connect", startTimeoutException.getMessage());
    }
}