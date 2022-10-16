package io.worldy.sockiopath;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.worldy.sockiopath.websocket.WebSocketServer;
import io.worldy.sockiopath.websocket.client.BootstrappedWebSocketClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.smallrye.common.constraint.Assert.assertTrue;

public class SockiopathServerTest {


    @Test
    void gracefulShutDownTest() throws InterruptedException, ExecutionException {

        Logger loggerMock = Mockito.mock(Logger.class);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        SockiopathServer webSocketServer =
                new WebSocketServer(
                        SockiopathServer.basicWebSocketChannelHandler(SockiopathServerTest::channelEchoWebSocketHandler),
                        executor,
                        0
                ) {
                    @Override
                    public Logger getLogger() {
                        return loggerMock;
                    }
                };

        startThenManuallyStopServer(webSocketServer);
        awaitTermination(executor);

        Mockito.verify(loggerMock, Mockito.atLeastOnce()).info("Graceful shutdown.");
        assertTerminationAndShutdown(executor);
    }

    @Test
    void gracefulDefaultShutDownTest() throws InterruptedException, ExecutionException {

        ExecutorService executor = Executors.newFixedThreadPool(1);
        SockiopathServer webSocketServer =
                new AbstractSockiopathServer(
                        SockiopathServer.basicWebSocketChannelHandler(SockiopathServerTest::channelEchoWebSocketHandler),
                        executor,
                        0
                ) {
                    @Override
                    public CompletableFuture<StartServerResult> start() {
                        CompletableFuture<StartServerResult> future = new CompletableFuture<>();
                        executorService.submit(() -> {
                            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
                            EventLoopGroup workerGroup = new NioEventLoopGroup();
                            try {
                                ServerBootstrap b = new ServerBootstrap();
                                b.group(bossGroup, workerGroup)
                                        .channel(NioServerSocketChannel.class)
                                        .handler(new LoggingHandler(LogLevel.INFO))
                                        .childHandler(channelHandler);

                                Channel channel = b.bind(port).sync().channel();
                                this.closeFuture = channel.closeFuture();
                                actualPort = SockiopathServer.getPort(channel);
                                future.complete(new StartServerResult(actualPort, closeFuture, this));
                                closeFuture.await();
                            } catch (Exception e) {
                                future.completeExceptionally(e);
                            } finally {
                                shutdownEventLoops(List.of(bossGroup, workerGroup));
                            }
                        });
                        return future;
                    }

                };

        webSocketServer.start().get().server().stop();
        awaitTermination(executor);
        assertTerminationAndShutdown(executor);
    }

    @Test
    void hastyDefaultShutDownTest() throws InterruptedException, ExecutionException {

        Logger loggerMock = Mockito.mock(Logger.class);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        SockiopathServer webSocketServer =
                new WebSocketServer(
                        SockiopathServer.basicWebSocketChannelHandler(SockiopathServerTest::channelEchoWebSocketHandler),
                        executor,
                        0
                ) {
                    @Override
                    public Logger getLogger() {
                        return loggerMock;
                    }

                    @Override
                    public long shutdownTimeoutMillis() {
                        return 1;
                    }
                };

        startThenManuallyStopServer(webSocketServer);
        awaitTermination(executor);

        Mockito.verify(loggerMock, Mockito.atLeastOnce()).warn("Hasty shutdown.");
        assertTerminationAndShutdown(executor);
    }

    @Test
    void hastyShutDownTest() throws InterruptedException, ExecutionException {

        Logger loggerMock = Mockito.mock(Logger.class);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        SockiopathServer webSocketServer =
                getWebSocketServerWithShutdownTimeouts(executor, loggerMock, 1, 50);

        startThenManuallyStopServer(webSocketServer);
        awaitTermination(executor);

        Mockito.verify(loggerMock, Mockito.atLeastOnce()).warn("Hasty shutdown.");
        assertTerminationAndShutdown(executor);
    }

    @Test
    void ungracefulShutDownTest() throws InterruptedException, ExecutionException {

        Logger loggerMock = Mockito.mock(Logger.class);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        SockiopathServer webSocketServer =
                getWebSocketServerWithShutdownTimeouts(executor, loggerMock, 0, 0);

        startThenManuallyStopServer(webSocketServer);
        awaitTermination(executor);

        Mockito.verify(loggerMock, Mockito.atLeastOnce()).error("Pool did not terminate.");
        assertTerminationAndShutdown(executor);
    }

    private static void assertTerminationAndShutdown(ExecutorService executor) {
        assertTrue(executor.isTerminated());
        assertTrue(executor.isShutdown());
    }

    private static void startThenManuallyStopServer(SockiopathServer webSocketServer) throws InterruptedException, ExecutionException {
        webSocketServer.start().get().closeFuture().cancel(false);
        webSocketServer.shutdownAndAwaitTermination();
    }

    @Test
    void interruptedShutDownTest() throws InterruptedException, ExecutionException {

        Logger loggerMock = Mockito.mock(Logger.class);
        List<Runnable> cancelledTasks = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(100);
        SockiopathServer webSocketServer =
                getWebSocketServerWithShutdownTimeouts(executor, loggerMock, 1, 1, true, cancelledTasks);

        webSocketServer.start().get().closeFuture().cancel(false);
        awaitTermination(executor);

        Mockito.verify(loggerMock, Mockito.atLeastOnce()).error("Interruption required during shutdown!");
        assertTerminationAndShutdown(executor);
        assertTrue(cancelledTasks.size() == 0);
    }

    private static void awaitTermination(ExecutorService executor) throws InterruptedException {
        if (!executor.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
            Assertions.fail("Executor did not terminate in time.");
        }
    }

    private static WebSocketServer getWebSocketServerWithShutdownTimeouts(
            ExecutorService executor,
            Logger loggerMock,
            int shutdownTimeout,
            int shutdownNowTimeout) {
        return getWebSocketServerWithShutdownTimeouts(executor, loggerMock, shutdownTimeout, shutdownNowTimeout, false, List.of());
    }

    private static WebSocketServer getWebSocketServerWithShutdownTimeouts(
            ExecutorService executor,
            Logger loggerMock,
            int shutdownTimeout,
            int shutdownNowTimeout,
            boolean forceInterrupt,
            List<Runnable> cancelledTasks) {
        return new WebSocketServer(
                SockiopathServer.basicWebSocketChannelHandler(SockiopathServerTest::channelEchoWebSocketHandler),
                executor,
                0
        ) {
            @Override
            public Logger getLogger() {
                return loggerMock;
            }

            @Override
            public long shutdownTimeoutMillis() {
                return shutdownTimeout;
            }

            @Override
            public long shutdownNowTimeoutMillis() {
                return shutdownNowTimeout;
            }

            @Override
            public void shutdownEventLoops(List<EventLoopGroup> groups) {
                super.shutdownEventLoops(groups);
                if (forceInterrupt) {
                    cancelledTasks.addAll(super.shutdownAndAwaitTermination());
                }
            }
        };
    }

    public static WebSocketServer getWebSocketServer(int port, SslContext serverSslContext) {
        final ChannelInitializer<SocketChannel> basicWebSocketChannelHandler;
        if (serverSslContext == null) {
            basicWebSocketChannelHandler = SockiopathServer.basicWebSocketChannelHandler(SockiopathServerTest::channelEchoWebSocketHandler);
        } else {
            basicWebSocketChannelHandler = SockiopathServer.basicWebSocketChannelHandler(SockiopathServerTest::channelEchoWebSocketHandler, serverSslContext);
        }
        return new WebSocketServer(
                basicWebSocketChannelHandler,
                Executors.newFixedThreadPool(1),
                port
        );
    }

    public static BootstrappedWebSocketClient getWebSocketClient(CountDownLatch latch, Map<Long, Object> responseMap, int port, SslContext clientSslContext) {
        return new BootstrappedWebSocketClient(
                "localhost",
                port,
                "/websocket",
                new CountDownLatchChannelHandler(latch, responseMap, (message) -> {
                }),
                clientSslContext,
                500,
                500
        );
    }

    public static SimpleChannelInboundHandler<Object> channelEchoWebSocketHandler() {
        return new SimpleChannelInboundHandler<>() {
            @Override
            protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object message) {
                if (message instanceof TextWebSocketFrame textFrame) {
                    String textMessage = textFrame.text();
                    channelHandlerContext.channel().writeAndFlush(new TextWebSocketFrame(textMessage));
                }
            }
        };
    }


}