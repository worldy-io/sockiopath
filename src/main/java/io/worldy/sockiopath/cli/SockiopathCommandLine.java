package io.worldy.sockiopath.cli;


import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.StartServerResult;
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

        Optional<StartServerResult> maybeWebSocketServer = Optional.ofNullable(
                startWebSocketServer(options, webSocketServerExecutorService, sessionStore)
        );

        AtomicInteger webSocketMessageIndexer = new AtomicInteger(-1);
        Map<Integer, Object> responseMap = new HashMap<>();
        Optional<Channel> maybeClientChannel = Optional.ofNullable(
                startWebSocketClient(
                        options,
                        maybeWebSocketServer.map(server -> server.port()).orElse(options.webSocketPort()),
                        webSocketMessageIndexer,
                        responseMap
                )
        );

//        var sessionId = maybeClientChannel.map(channel -> {
//            channel.writeAndFlush("join");
//
//            System.out.println("sessionId: " + responseMap.get(0));
//            return responseMap.get(0);
//        });


        boolean quit = false;
        int lastMessageIndex = -1;
        while (!quit) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in));

            String command = reader.readLine();
            System.out.println("command: " + command);

            maybeClientChannel.ifPresent(channel -> {
                channel.writeAndFlush(new TextWebSocketFrame(command));
            });

            if(responseMap.get(0) != null) {
                System.out.println(((TextWebSocketFrame)responseMap.get(0)).text());
            }

            if (command.equals("quit")) {
                System.out.println("stopping WebSocket server...");
                maybeWebSocketServer.map(s -> s.closeFuture().cancel(true));
                webSocketServerExecutorService.shutdown();
                shutdownAndAwaitTermination(webSocketServerExecutorService);
                System.out.println("WebSocket server stopped");
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

    static StartServerResult startWebSocketServer(Options options, ExecutorService webSocketServerExecutorService, SessionStore<SockiopathSession> sessionStore) throws ExecutionException, InterruptedException {
        final StartServerResult startWebSocketServerResult;
        if (options.server()) {
            SockiopathServer sever = webSocketServer(options, webSocketServerExecutorService, sessionStore, DELIMINATOR);
            startWebSocketServerResult = sever.start().orTimeout(1000, TimeUnit.MILLISECONDS).get();
            System.out.println("Started WebSocket server on port: " + startWebSocketServerResult.port());
        } else {
            startWebSocketServerResult = null;
        }
        return startWebSocketServerResult;
    }

    static void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(5, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    static SockiopathServer webSocketServer(Options options, ExecutorService executor, SessionStore<SockiopathSession> sessionStore, char deliminator) {


        List<Supplier<SimpleChannelInboundHandler<?>>> messageHandlerSupplier = List.of(
                () -> new WebSocketIndexPageHandler(SockiopathServer.DEFAULT_WEB_SOCKET_PATH, "ui/websockets.html"),
                () -> new WebSocketHandler(sessionStore, getMessageHandlers(), deliminator)
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
