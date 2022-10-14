package io.worldy.sockiopath.messaging;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.function.Function;

public class DefaultMessageParser implements Function<ByteBuffer, Optional<SockiopathMessage>> {

    private final Character deliminator;

    public DefaultMessageParser(Character deliminator) {
        this.deliminator = deliminator;
    }

    @Override
    public Optional<SockiopathMessage> apply(ByteBuffer content) {
        var capacity = content.capacity();
        content.position(0);

        return getPart(content, capacity)
                .flatMap(address ->
                        getPart(content, capacity).map(sessionId ->
                                new SockiopathMessage(address, sessionId, getData(content, capacity))
                        )
                );
    }

    protected byte[] getData(ByteBuffer content, int capacity) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (content.position() < capacity) {
            baos.write(content.get());
        }
        return baos.toByteArray();
    }

    protected Optional<String> getPart(ByteBuffer content, int capacity) {
        StringBuilder addressBuilder = new StringBuilder();
        boolean delimiterFound = false;
        while (content.position() < capacity) {
            char character = (char) content.get();
            if (deliminator.equals(character)) {
                delimiterFound = true;
                break;
            }
            addressBuilder.append(character);
        }
        return delimiterFound ? Optional.of(addressBuilder.toString()) : Optional.empty();
    }

}
