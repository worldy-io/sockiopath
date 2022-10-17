package io.worldy.sockiopath.websocket.client;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.worldy.sockiopath.messaging.DefaultMessageParser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.Map;

import static io.worldy.sockiopath.SockiopathHandlerTest.getMessageHandlers;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WebSocketClientHandlerTest {

    @Test
    void isNotUdpTest() {
        assertFalse(new WebSocketClientHandler(Map.of()).isUdp());
    }

    @Test
    void channelRead0TextTest() {
        Logger loggerMock = Mockito.mock(Logger.class);
        WebSocketClientHandler handler =
                new WebSocketClientHandler(getMessageHandlers(), new DefaultMessageParser('|'), loggerMock);

        TextWebSocketFrame frame = new TextWebSocketFrame("hi");
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(context.channel()).thenReturn(channel);
        handler.channelRead0(context, frame);
        Mockito.verify(loggerMock, Mockito.times(1)).debug("MESSAGE received: hi");
    }

    @Test
    void channelRead0BinaryTest() {
        Logger loggerMock = Mockito.mock(Logger.class);
        WebSocketClientHandler handler =
                new WebSocketClientHandler(getMessageHandlers(), new DefaultMessageParser('|'), loggerMock);

        BinaryWebSocketFrame frame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer("address-empty||payload".getBytes()));
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(context.channel()).thenReturn(channel);
        handler.channelRead0(context, frame);

        Mockito.verify(loggerMock, Mockito.times(1)).debug("BINARY received");
        Mockito.verify(loggerMock, Mockito.times(1)).debug("No response from message bus: address-empty");
    }

    @Test
    void channelRead0UnsupportedFrameTypeTest() {
        Logger loggerMock = Mockito.mock(Logger.class);
        WebSocketClientHandler handler =
                new WebSocketClientHandler(getMessageHandlers(), new DefaultMessageParser('|'), loggerMock);

        WebSocketFrame frame = Mockito.mock(PingWebSocketFrame.class);
        ChannelHandlerContext context = Mockito.mock(ChannelHandlerContext.class);
        handler.channelRead0(context, frame);

        Mockito.verify(context, Mockito.never()).channel();
        Mockito.verifyNoInteractions(context);
        Mockito.verify(loggerMock, Mockito.times(1)).error("Unsupported frame type: io.netty.handler.codec.http.websocketx.PingWebSocketFrame");
    }
}