package io.worldy.sockiopath.cli;


import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.session.MapBackedSessionStore;
import io.worldy.sockiopath.session.SessionStore;
import io.worldy.sockiopath.session.SockiopathSession;
import io.worldy.sockiopath.websocket.WebSocketServer;
import org.apache.commons.cli.HelpFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SockiopathCommandLine {

    private static final Logger logger = LoggerFactory.getLogger(SockiopathCommandLine.class);

    private static final char DELIMINATOR = '|';
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

        SessionStore<SockiopathSession> sessionStore = new MapBackedSessionStore(new HashMap<>());

        ExecutorService webSocketServerExecutorService = Executors.newFixedThreadPool(1);

        Optional<SockiopathServer> maybeWebSocketServer = Optional.ofNullable(
                startWebSocketServer(options, webSocketServerExecutorService, sessionStore)
        );

        if (maybeWebSocketServer.isEmpty()) {
            return "No server or client was started. Closing CLI.";
        }

        boolean quit = false;
        while (!quit) {
            String command = reader.readLine();
            if (command == null) {
                continue;
            }
            System.out.println("command: " + command);

            //            maybeClientChannel.ifPresent(channel -> {
            //                channel.writeAndFlush(new TextWebSocketFrame(command));
            //            });
            //
            //            if(responseMap.get(0) != null) {
            //                System.out.println(((TextWebSocketFrame)responseMap.get(0)).text());
            //            }

            if (command.equals("quit")) {
                maybeWebSocketServer.ifPresent(SockiopathServer::stop);
                quit = true;
            }
        }
        return "stopping WebSocket server...";
    }


//    private static Channel startWebSocketClient(Options options, int port, AtomicInteger messageIndexer, Map<Integer, Object> responseMap) throws InterruptedException {
//        final Channel channel;
//        if (options.client()) {
//            var client = new BootstrappedWebSocketClient(
//                    "localhost",
//                    port,
//                    "/websocket",
//                    new CommandLineClientHandler(messageIndexer, responseMap),
//                    null,
//                    500,
//                    500
//            );
//            client.startup();
//            channel = client.getChannel();
//        } else {
//            channel = null;
//        }
//        return channel;
//    }

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


        List<Supplier<SimpleChannelInboundHandler<?>>> messageHandlerSupplier = List.of();

//        List.of(
//                () -> new WebSocketIndexPageHandler(SockiopathServer.DEFAULT_WEB_SOCKET_PATH, "ui/websockets.html"),
//                () -> new WebSocketHandler(sessionStore, getMessageHandlers(), DELIMINATOR)
//        );

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

//    static Map<String, MessageBus> getMessageHandlers() {
//        return Map.of(
//                "ping", new MessageBus((sockiopathMessage) -> {
//                    return CompletableFuture.completedFuture("pong".getBytes());
//                }, 1000)
//        );
//    }
}
