package org.github.ponking66;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.github.ponking66.protoctl.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author pony
 * @date 2023/5/10
 */
public class NettyDecoderEncoderHandlerTest {

    @Test
    public void test() throws Exception {
        NettyMessageEncoder encoder = new NettyMessageEncoder();
        NettyMessageDecoder decoder = new NettyMessageDecoder();
        EmbeddedChannel channel = new EmbeddedChannel(decoder, encoder);
        NettyMessage msg1 = new NettyMessage();
        msg1.setHeader(new Header()
                .setSessionID(System.currentTimeMillis())
                .setMagic(0xabcdf123)
                .setVersion((byte) 2)
                .setType(MessageType.LOGIN_RESPONSE).setVersion((byte) 1));
        msg1.setBody("Good morning sir!");

        Assert.assertTrue(channel.writeOutbound(msg1));
        ByteBuf byteBuf = channel.readOutbound();

        Assert.assertTrue(channel.writeInbound(byteBuf));
        NettyMessage msg2 = channel.readInbound();

        Assert.assertEquals(msg1.getHeader().getType(), msg2.getHeader().getType());
//        Assert.assertEquals(msg1.getHeader().getLength(), msg2.getHeader().getLength());
        Assert.assertEquals(msg1.getHeader().getVersion(), msg2.getHeader().getVersion());
        Assert.assertEquals(msg1.getHeader().getMagic(), msg2.getHeader().getMagic());
        Assert.assertEquals(msg1.getHeader().getSessionID(), msg2.getHeader().getSessionID());
        Assert.assertEquals(msg1.getBody(), msg2.getBody());
    }
}
