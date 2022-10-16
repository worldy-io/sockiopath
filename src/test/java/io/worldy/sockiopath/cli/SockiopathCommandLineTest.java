package io.worldy.sockiopath.cli;

import io.worldy.sockiopath.SockiopathServer;
import org.apache.commons.cli.HelpFormatter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.fail;

class SockiopathCommandLineTest {

    //private static final Logger logger = LoggerFactory.getLogger(SockiopathCommandLineTest.class);

    @Test
    void run() {
    }

    @Test
    void mainInterruptedTest() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            try {
                SockiopathCommandLine.main(getArgs(getStandardArgs()));
            } catch (ExecutionException | InterruptedException | IOException e) {
                throw new RuntimeException("Unexpected exception caught in test task.", e);
            }
        });

        Logger loggerMock = Mockito.mock(Logger.class);

        SockiopathServer.shutdownAndAwaitTermination(executorService, 0L, 1L, loggerMock);

        if (!executorService.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
            fail("Server took too long to shutdown from CLI.");
        }

        Mockito.verify(loggerMock, Mockito.atLeastOnce()).info("done shutting down ExecutorService.");
    }

    @Test
    void mainQuitGracefullyTest() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            try {
                String[] args = {"-s", "false", "-c", "false"};
                SockiopathCommandLine.main(args);
            } catch (ExecutionException | InterruptedException | IOException e) {
                throw new RuntimeException("Unexpected exception caught in test task.", e);
            }
        });

        Logger loggerMock = Mockito.mock(Logger.class);

        SockiopathServer.shutdownAndAwaitTermination(executorService, 0L, 1L, loggerMock);

        if (!executorService.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
            fail("Server took too long to shutdown from CLI.");
        }

        Mockito.verify(loggerMock, Mockito.atLeastOnce()).info("done shutting down ExecutorService.");
    }

    @Test
    void runQuitGracefullyTest() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            try {
                SockiopathCommandLine sockiopathCommandLine = new SockiopathCommandLine(getOptions(getStandardArgs()));
                BufferedReader reader = Mockito.mock(BufferedReader.class);
                Mockito.when(reader.readLine()).thenReturn("quit");
                sockiopathCommandLine.run(reader);
                executorService.shutdownNow();
            } catch (ExecutionException | InterruptedException | IOException e) {
                throw new RuntimeException("Unexpected exception caught in test task.", e);
            }
        });

        if (!executorService.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
            fail("Server took too long to shutdown from CLI.");
        }
    }


    @Test
    void runUnknownCommandTest() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            try {
                SockiopathCommandLine sockiopathCommandLine = new SockiopathCommandLine(getOptions(getStandardArgs()));
                BufferedReader reader = Mockito.mock(BufferedReader.class);
                Mockito.when(reader.readLine()).thenAnswer(new Answer<String>() {
                    private int count = 0;

                    public String answer(InvocationOnMock invocation) {
                        if (count++ == 1)
                            return 1 + "";

                        return null;
                    }
                });
                sockiopathCommandLine.run(reader);
                executorService.shutdownNow();
            } catch (ExecutionException | InterruptedException | IOException e) {
                throw new RuntimeException("Unexpected exception caught in test task.", e);
            }
        });

        if (executorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
            fail("Server should not have shut down!");
        }
    }


    @Test
    void runUnknownCommandThenQuitTest() throws InterruptedException, IOException, URISyntaxException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);

        executorService.submit(() -> {
            try {
                SockiopathCommandLine sockiopathCommandLine = new SockiopathCommandLine(getOptions(getStandardArgs()));
                BufferedReader reader = Mockito.mock(BufferedReader.class);
                Mockito.when(reader.readLine()).thenReturn("test1", null, "quit");
                sockiopathCommandLine.run(reader);
                executorService.shutdownNow();
            } catch (ExecutionException | InterruptedException | IOException e) {
                throw new RuntimeException("Unexpected exception caught in test task.", e);
            }
        });

        if (!executorService.awaitTermination(10000, TimeUnit.MILLISECONDS)) {
            fail("Server took too long to shutdown from CLI.");
        }
    }


    @Test
    void noServerOrClientStartedQuitGracefullyTest() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Logger mockLogger = Mockito.mock(Logger.class);
        executorService.submit(() -> {
            try {
                List<String> additionalArgs = List.of("-s", "false", "-c", "false");
                Options options = getOptions(addToStandardArgs(additionalArgs));
                SockiopathCommandLine sockiopathCommandLine = new SockiopathCommandLine(options);
                mockLogger.info(sockiopathCommandLine.run(Mockito.mock(BufferedReader.class)));
                executorService.shutdownNow();
            } catch (ExecutionException | InterruptedException | IOException e) {
                throw new RuntimeException("Unexpected exception caught in test task.", e);
            }
        });

        if (!executorService.awaitTermination(10000, TimeUnit.MILLISECONDS)) {
            fail("Server took too long to shutdown from CLI.");
        }
        Mockito.verify(mockLogger, Mockito.times(1)).info("No server or client was started. Closing CLI.");
    }

    private List<String> addToStandardArgs(List<String> additionalArgs) {
        ArrayList<String> args = new ArrayList<>(additionalArgs);
        args.addAll(getStandardArgs());
        return args;
    }

    private List<String> getStandardArgs() {
        return List.of("-wsPort", "0", "-udpPort", "0");
    }

    private static Options getOptions(List<String> args) {
        return Options.parse(getArgs(args), new HelpFormatter());
    }

    private static String[] getArgs(List<String> args) {
        return args.toArray(new String[0]);
    }

    @Test
    void startWebSocketServer() {
    }

    @Test
    void webSocketServer() {
    }

    @Test
    void getMessageHandlers() {
    }
}