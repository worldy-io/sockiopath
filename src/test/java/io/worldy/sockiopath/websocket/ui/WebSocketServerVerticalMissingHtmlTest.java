package io.worldy.sockiopath.websocket.ui;


import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.worldy.sockiopath.SockiopathServer;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(WebSocketServerVerticalMissingHtmlTest.BuildTimeValueChangeTestProfile.class)
class WebSocketServerVerticalMissingHtmlTest {

    public static class BuildTimeValueChangeTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.log.category.\"io.worldy.sockiopath.websocket.ui\".level", "OFF"
            );
        }
    }

    @Test
    public void webSocketMissingHtmlTest() throws Exception {
        SockiopathServer webSocketServer = WebSocketServerVerticalHtmlTest.getWebSocketServer(0, "io/worldy/sockiopath/websocket/ui/missing.html");
        int port = webSocketServer.start().orTimeout(1000, TimeUnit.MILLISECONDS).get().port();

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port))
                .header("accept", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("WebSocket HTML content was missing.", response.body());
    }
}
