package io.worldy.sockiopath.cli;


import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.worldy.sockiopath.SockioPathClient;
import io.worldy.sockiopath.Sockiopath;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.messaging.MessageBus;
import io.worldy.sockiopath.session.MapBackedSessionStore;
import io.worldy.sockiopath.session.SessionStore;
import io.worldy.sockiopath.session.SockiopathSession;
import io.worldy.sockiopath.websocket.WebSocketServer;
import io.worldy.sockiopath.websocket.WebSocketServerHandler;
import io.worldy.sockiopath.websocket.ui.WebSocketIndexPageHandler;
import org.apache.commons.cli.HelpFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SockiopathCommandLine {

    private static final Logger logger = LoggerFactory.getLogger(SockiopathCommandLine.class);
    private final Options options;

    public SockiopathCommandLine(Options options) {
        this.options = options;
    }

    public static void main(String args[]) throws ExecutionException, InterruptedException, IOException {
        Options options = Options.parse(args, new HelpFormatter());
        String response = new SockiopathCommandLine(options).run();
        System.out.println(response);
    }

    public String run() throws ExecutionException, InterruptedException, IOException {
        InputStreamReader inputStream = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(inputStream);
        return run(reader);
    }


    public String run(BufferedReader reader) throws ExecutionException, InterruptedException, IOException {

        logger.debug("webSocketHost: " + options.webSocketHost());
        logger.debug("webSocketPort: " + options.webSocketPort());
        logger.debug("udpHost: " + options.udpHost());
        logger.debug("udpPort: " + options.udpPort());
        logger.debug("client: " + options.client());
        logger.debug("server: " + options.server());

        SessionStore<SockiopathSession> sessionStore = new MapBackedSessionStore(new HashMap<>());

        ExecutorService webSocketServerExecutorService = Executors.newFixedThreadPool(1);

        Optional<SockiopathServer> maybeWebSocketServer = Optional.ofNullable(
                startWebSocketServer(options, webSocketServerExecutorService, sessionStore)
        );

        Optional<SockioPathClient> maybeWebSocketClient = Optional.ofNullable(
                startWebSocketClient(options, maybeWebSocketServer.map(SockiopathServer::actualPort).orElse(options.webSocketPort()))
        );

        if (maybeWebSocketServer.isEmpty() && maybeWebSocketClient.isEmpty()) {
            return "No server or client was started. Closing CLI.";
        }

        boolean quit = false;
        while (!quit) {
            String command = reader.readLine();
            if (command == null) {
                continue;
            }

            //System.out.println("command: " + command);

            if (command.equals("quit")) {
                maybeWebSocketServer.ifPresent(SockiopathServer::stop);
                maybeWebSocketClient.ifPresent(SockioPathClient::shutdownClient);
                quit = true;
            } else {
                maybeWebSocketClient.ifPresent(client -> {
                    client.sendWebSocketMessage("chat", command);
                });
            }
        }
        return "stopping Sockiopath CLI...";
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
                () -> new WebSocketServerHandler(sessionStore, getMessageHandlers())
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
//                "chat", new MessageBus((sockiopathMessage) -> {
//                    return CompletableFuture.completedFuture("hi".getBytes());
//                }, 1000)
        );
    }


    static SockioPathClient startWebSocketClient(
            Options options,
            int webSocketPort
    ) throws InterruptedException {
        final SockioPathClient sockiopathclient;
        if (options.client()) {
            sockiopathclient = Sockiopath.sockioPathClient();

            sockiopathclient.registerClientMessageHandler(
                    "chat",
                    System.out::println,
                    String::new
            );

            sockiopathclient.connectClient(webSocketPort, 0);
        } else {
            sockiopathclient = null;
        }
        return sockiopathclient;
    }
}
