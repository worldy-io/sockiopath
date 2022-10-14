package io.worldy.sockiopath.websocket.ui;


import io.netty.channel.SimpleChannelInboundHandler;
import io.quarkus.test.junit.QuarkusTest;
import io.worldy.sockiopath.SockiopathServer;
import io.worldy.sockiopath.websocket.WebSocketServer;
import io.worldy.sockiopath.websocket.WebSocketServerTest;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class WebSocketServerVerticalHtmlTest {

    @Test
    public void webSocketHtmlTest() throws Exception {
        testWebSocketHtml("");
    }

    @Test
    public void webSocketIndexHtmlTest() throws Exception {
        testWebSocketHtml("/index.html");
    }

    private void testWebSocketHtml(String path) throws Exception {
        SockiopathServer webSocketServer = getWebSocketServer(0, "io/worldy/sockiopath/websocket/ui/websockets.html");

        int port = webSocketServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get().port();

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + path))
                .header("accept", "application/json")
                .build();

        String expectedHtml = EXPECTED_HTML.formatted(port);

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals(StringUtils.deleteWhitespace(expectedHtml), StringUtils.deleteWhitespace(response.body()));
    }

    @Test
    public void webSocketNotFoundTest() throws Exception {

        SockiopathServer webSocketServer = getWebSocketServer(0, "io/worldy/sockiopath/websocket/ui/websockets.html");
        int port = webSocketServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get().port();

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/unknown-resource"))
                .header("accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
        assertEquals("404 Not Found", response.body());
    }

    @Test
    public void webSocketPostMethodNotAllowedTest() throws Exception {
        methodNotAllowedTest(HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.noBody()));
    }

    @Test
    public void webSocketPutMethodNotAllowedTest() throws Exception {
        methodNotAllowedTest(HttpRequest.newBuilder().PUT(HttpRequest.BodyPublishers.noBody()));
    }

    @Test
    public void webSocketDeleteMethodNotAllowedTest() throws Exception {
        methodNotAllowedTest(HttpRequest.newBuilder().DELETE());
    }

    private void methodNotAllowedTest(HttpRequest.Builder requestBuilder) throws Exception {
        SockiopathServer webSocketServer = getWebSocketServer(0, "io/worldy/sockiopath/websocket/ui/websockets.html");
        int port = webSocketServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get().port();

        HttpClient client = HttpClient.newHttpClient();
        requestBuilder.uri(URI.create("http://localhost:" + port));

        HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, response.statusCode());
        assertEquals("403 Forbidden", response.body());
    }


    static WebSocketServer getWebSocketServer(int port, String htmlTemplatePath) {
        List<Supplier<SimpleChannelInboundHandler<?>>> messageHandlerSupplier = List.of(
                () -> new WebSocketIndexPageHandler(SockiopathServer.DEFAULT_WEB_SOCKET_PATH, htmlTemplatePath),
                WebSocketServerTest::channelEchoHandler
        );

        return new WebSocketServer(
                SockiopathServer.basicWebSocketChannelHandler(
                        messageHandlerSupplier,
                        null
                ),
                Executors.newFixedThreadPool(1),
                port
        );
    }

    private static final String EXPECTED_HTML = """
            <html>
            <head><title>Web Socket Test</title></head>
            <body>
            <script type="text/javascript">
                var socket;
                if (!window.WebSocket) {
                    window.WebSocket = window.MozWebSocket;
                }
                if (window.WebSocket) {
                    socket = new WebSocket("ws://localhost:%d/websocket");
                    socket.onmessage = function (event) {
                        var ta = document.getElementById('responseText');
                        ta.value = ta.value + '\\n' + event.data
                    };
                    socket.onopen = function (event) {
                        var ta = document.getElementById('responseText');
                        ta.value = "Web Socket opened!";
                    };
                    socket.onclose = function (event) {
                        var ta = document.getElementById('responseText');
                        ta.value = ta.value + "Web Socket closed";
                    };
                } else {
                    alert("Your browser does not support Web Socket.");
                }
                        
                function send(message) {
                    if (!window.WebSocket) {
                        return;
                    }
                    if (socket.readyState == WebSocket.OPEN) {
                        socket.send(message);
                    } else {
                        alert("The socket is not open.");
                    }
                }
            </script>
            <form onsubmit="return false;">
                <input type="text" name="message" value="Hello, World!"/><input type="button" value="Send Web Socket Data"
                                                                                onclick="send(this.form.message.value)"/>
                <h3>Output</h3>
                <textarea id="responseText" style="width:500px;height:300px;"></textarea>
            </form>
            </body>
            </html>""";
}
