package io.worldy.sockiopath.messaging;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DefaultMessageParserTest {

    @Test
    void apply() {
        DefaultMessageParser defaultMessageParser = new DefaultMessageParser('|');

        SockiopathMessage parsedMessage = defaultMessageParser.apply(ByteBuffer.wrap("address|sessionId|data".getBytes()))
                .orElseThrow();

        assertEquals("address", parsedMessage.address());
        assertEquals("sessionId", parsedMessage.sessionId());
        assertEquals("data", new String(parsedMessage.data()));
    }
}