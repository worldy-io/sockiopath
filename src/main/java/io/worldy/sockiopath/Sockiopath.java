package io.worldy.sockiopath;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.worldy.sockiopath.messaging.MessageBus;
import io.worldy.sockiopath.websocket.client.BootstrappedWebSocketClient;
import io.worldy.sockiopath.websocket.client.WebSocketClientHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class Sockiopath implements SockioPathClient {

    Map<String, MessageBus> clientMessageHandlers = new HashMap<>();

    BootstrappedWebSocketClient webSocketClient = null;

    public static SockioPathClient sockioPathClient() {
        return new Sockiopath();
    }

    @Override
    public <T> void registerClientMessageHandler(String address, Consumer<T> handler, Function<byte[], T> parser) {
        MessageBus messageBus = new MessageBus(
//                (sm) -> {
//                    handler.accept(parser.apply(sm.data()));
//                    return CompletableFuture.completedFuture(sm.data());
//                },
                null,
                1000
        );
        clientMessageHandlers.put(address, messageBus);
    }

    @Override
    public void connectClient(int webSocketPort, int udpPort, Consumer<TextWebSocketFrame> onTextFrame) throws InterruptedException {

        webSocketClient = new BootstrappedWebSocketClient(
                "localhost",
                webSocketPort,
                SockiopathServer.DEFAULT_WEB_SOCKET_PATH,
                new WebSocketClientHandler(clientMessageHandlers) {
                    @Override
                    protected void handleTextFrame(TextWebSocketFrame textFrame) {
                        super.handleTextFrame(textFrame);
                        onTextFrame.accept(textFrame);
                    }
                },
                null,
                1000,
                1000
        );
        webSocketClient.startup();

    }

    @Override
    public void sendWebSocketMessage(String address, String message) {
        webSocketClient.getChannel().writeAndFlush(new TextWebSocketFrame(message));
    }

//    @Override
//    public void sendUdpMessage(String address, byte[] payload) {
//
//    }

    @Override
    public void shutdownClient() {
        webSocketClient.shutdown();
    }
}
