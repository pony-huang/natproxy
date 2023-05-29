package org.github.ponking66.protoctl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.jboss.marshalling.Marshaller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author pony
 * @date 2023/4/27
 */
public final class NettyMessageEncoder extends MessageToByteEncoder<NettyMessage> {


    private final Marshaller marshaller;

    private static final byte[] LENGTH_PLACEHOLDER = new byte[4];

    public NettyMessageEncoder() throws IOException {
        this.marshaller = MarshallingCodecFactory.buildMarshalling();
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, NettyMessage msg, ByteBuf byteBuf) throws Exception {

        if (msg == null || msg.getHeader() == null) {
            //TODO throw new Exception
            throw new RuntimeException("The encode message is null.");
        }

        long sessionID = System.currentTimeMillis();
        msg.getHeader().setSessionID(sessionID);


        byteBuf.writeInt(msg.getHeader().getMagic()); // 4
        byteBuf.writeInt(msg.getHeader().getLength()); // 4
        byteBuf.writeLong(msg.getHeader().getSessionID()); // 8
        byteBuf.writeByte(msg.getHeader().getType()); // 1
        byteBuf.writeByte(msg.getHeader().getVersion()); // 1
        byteBuf.writeInt(msg.getHeader().getStatus()); // 1
        byteBuf.writeInt(msg.getHeader().getExtended().size()); // 4

        String key;
        byte[] keyContent;
        Object value;

        for (Map.Entry<String, Object> param : msg.getHeader().getExtended().entrySet()) {
            key = param.getKey();
            keyContent = key.getBytes(StandardCharsets.UTF_8);
            byteBuf.writeInt(keyContent.length);
            byteBuf.writeBytes(keyContent);

            value = param.getValue();
            encode(value, byteBuf);
        }

        if (msg.getBody() != null) {
            encode(msg.getBody(), byteBuf);
        } else {
            byteBuf.writeInt(0);
        }

        byteBuf.setInt(4, byteBuf.readableBytes());
    }

    public void encode(Object msg, ByteBuf out) throws Exception {
        try {
            int lengthPos = out.writerIndex();
            out.writeBytes(LENGTH_PLACEHOLDER);
            ChannelBufferByteOutput output = new ChannelBufferByteOutput(out);
            marshaller.start(output);
            marshaller.writeObject(msg);
            marshaller.finish();
            out.setInt(lengthPos, out.writerIndex() - lengthPos - 4);
        } finally {
            marshaller.clearClassCache();
            marshaller.clearInstanceCache();
            marshaller.close();

        }
    }
}
