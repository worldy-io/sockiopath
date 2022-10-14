package io.worldy.sockiopath.websocket.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

@QuarkusTest
@TestProfile(HandshakerChannelHandlerTest.BuildTimeValueChangeTestProfile.class)
class HandshakerChannelHandlerTest {
    public static class BuildTimeValueChangeTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.log.category.\"io.worldy.sockiopath.websocket.client.HandshakerChannelHandler\".level", "OFF"
            );
        }
    }
    @Test
    void channelReadHandshakeFailTest() throws Exception {
        WebSocketClientHandshaker handshaker = Mockito.mock(WebSocketClientHandshaker.class);
        Mockito.when(handshaker.isHandshakeComplete()).thenReturn(false);

        WebSocketHandshakeException webSocketHandshakeException = new WebSocketHandshakeException("Mocked handshake!");
        Mockito.doThrow(webSocketHandshakeException)
                .when(handshaker).finishHandshake(Mockito.any(), Mockito.any());

        HandshakerChannelHandler handshakerChannelHandler = new HandshakerChannelHandler(handshaker, null);

        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(ctx.channel()).thenReturn(channel);
        ChannelPromise channelPromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(ctx.newPromise()).thenReturn(channelPromise);

        handshakerChannelHandler.handlerAdded(ctx);
        handshakerChannelHandler.channelRead0(ctx, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));

        Mockito.verify(channelPromise, Mockito.times(1)).setFailure(webSocketHandshakeException);

    }

    @Test
    public void exceptionCaughtAfterHandshakeCompleteTest() {
        testExceptionCaught(true);
    }

    @Test
    public void exceptionCaughtBeforeHandshakeCompleteTest() {
        testExceptionCaught(false);
    }


    public void testExceptionCaught(boolean handshakeDone) {

        WebSocketClientHandshaker handshaker = Mockito.mock(WebSocketClientHandshaker.class);
        HandshakerChannelHandler handshakerChannelHandler = new HandshakerChannelHandler(handshaker, null);


        ChannelHandlerContext ctx = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(ctx.channel()).thenReturn(channel);
        ChannelPromise channelPromise = Mockito.mock(ChannelPromise.class);
        Mockito.when(channelPromise.isDone()).thenReturn(handshakeDone);
        Mockito.when(ctx.newPromise()).thenReturn(channelPromise);

        handshakerChannelHandler.handlerAdded(ctx);
        RuntimeException handshakeException = new RuntimeException("intentional exception");
        handshakerChannelHandler.exceptionCaught(ctx, handshakeException);

        Mockito.verify(ctx, Mockito.times(1)).close();
        if (!handshakeDone) {
            Mockito.verify(channelPromise, Mockito.times(1)).setFailure(handshakeException);
        }
    }
}
