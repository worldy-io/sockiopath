package io.worldy.sockiopath.cli;


import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.messaging.MessageBus;
import io.worldy.sockiopath.session.MapBackedSessionStore;
import io.worldy.sockiopath.session.SessionStore;
import io.worldy.sockiopath.session.SockiopathSession;
import io.worldy.sockiopath.websocket.WebSocketHandler;
import io.worldy.sockiopath.websocket.WebSocketServer;
import io.worldy.sockiopath.websocket.client.BootstrappedWebSocketClient;
import io.worldy.sockiopath.websocket.ui.WebSocketIndexPageHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class SockiopathCommandLine {

    private static final char DELIMINATOR = '|';


    public static void main(String args[]) throws ExecutionException, InterruptedException, IOException {

        Options options = Options.parse(args);

        SessionStore<SockiopathSession> sessionStore = new MapBackedSessionStore(new HashMap<>());

        ExecutorService webSocketServerExecutorService = Executors.newFixedThreadPool(1);

        Optional<SockiopathServer> maybeWebSocketServer = Optional.ofNullable(
                startWebSocketServer(options, webSocketServerExecutorService, sessionStore)
        );

        boolean quit = false;
        while (!quit) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in));

            String command = reader.readLine();
            System.out.println("command: " + command);

//            maybeClientChannel.ifPresent(channel -> {
//                channel.writeAndFlush(new TextWebSocketFrame(command));
//            });
//
//            if(responseMap.get(0) != null) {
//                System.out.println(((TextWebSocketFrame)responseMap.get(0)).text());
//            }

            if (command.equals("quit")) {
                System.out.println("stopping WebSocket server...");
                maybeWebSocketServer.ifPresent(SockiopathServer::stop);
                quit = true;
            }
        }
    }

    private static Channel startWebSocketClient(Options options, int port, AtomicInteger messageIndexer, Map<Integer, Object> responseMap) throws InterruptedException {
        final Channel channel;
        if (options.client()) {
            var client = new BootstrappedWebSocketClient(
                    "localhost",
                    port,
                    "/websocket",
                    new CommandLineClientHandler(messageIndexer, responseMap),
                    null,
                    500,
                    500
            );
            client.startup();
            channel = client.getChannel();
        } else {
            channel = null;
        }
        return channel;
    }

    static SockiopathServer startWebSocketServer(Options options, ExecutorService webSocketServerExecutorService, SessionStore<SockiopathSession> sessionStore) throws ExecutionException, InterruptedException {
        final SockiopathServer sockiopathServer;
        if (options.server()) {
            sockiopathServer = webSocketServer(options, webSocketServerExecutorService, sessionStore);
            var port = sockiopathServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get().port();
            System.out.println("Started WebSocket server on port: " + port);
        } else {
            sockiopathServer = null;
        }
        return sockiopathServer;
    }

    static SockiopathServer webSocketServer(Options options, ExecutorService executor, SessionStore<SockiopathSession> sessionStore) {


        List<Supplier<SimpleChannelInboundHandler<?>>> messageHandlerSupplier = List.of(
                () -> new WebSocketIndexPageHandler(SockiopathServer.DEFAULT_WEB_SOCKET_PATH, "ui/websockets.html"),
                () -> new WebSocketHandler(sessionStore, getMessageHandlers(), DELIMINATOR)
        );

        ChannelInitializer<SocketChannel> newHandler = SockiopathServer.basicWebSocketChannelHandler(
                messageHandlerSupplier,
                null
        );

        return new WebSocketServer(
                newHandler,
                executor,
                options.webSocketPort()
        );
    }

    static Map<String, MessageBus> getMessageHandlers() {
        return Map.of(
                "ping", new MessageBus((sockiopathMessage) -> {
                    return CompletableFuture.completedFuture("pong".getBytes());
                }, 1000)
        );
    }
}
