package io.worldy.sockiopath;

import java.util.function.Consumer;
import java.util.function.Function;

public interface SockioPathClient {
    <Response> void registerClientMessageHandler(String address, Consumer<Response> handler, Function<byte[], Response> parser);

    void connectClient(int webSocketPort, int udpPort) throws InterruptedException;

    void sendWebSocketMessage(String address, String message);

    //void sendUdpMessage(String address, byte[] payload);

    void shutdownClient();
}
