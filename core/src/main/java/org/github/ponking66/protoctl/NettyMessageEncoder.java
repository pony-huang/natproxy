package org.github.ponking66.protoctl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.github.ponking66.pojo.BodyBuffer;

/**
 * @author pony
 * @date 2023/4/27
 */
public final class NettyMessageEncoder extends MessageToByteEncoder<NettyMessage> {


    private static final byte[] LENGTH_PLACEHOLDER = new byte[4];

    /**
     * 协议例子如下
     * BEFORE DECODE (17 bytes)                      AFTER DECODE (17 bytes)
     * +----------+----------+----------------+      +----------+----------+----------------+
     * | meta     |  Length  | Actual Content |----->| meta | Length | Actual Content |
     * |  0xCAFE  | 12       | "HELLO, WORLD" |      |  0xCAFE  | 12       | "HELLO, WORLD" |
     * +----------+----------+----------------+      +----------+----------+----------------+
     *
     * @param channelHandlerContext the {@link ChannelHandlerContext} which this {@link MessageToByteEncoder} belongs to
     * @param msg                   the message to encode
     * @param byteBuf               the {@link ByteBuf} into which the encoded message will be written
     */
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, NettyMessage msg, ByteBuf byteBuf) throws Exception {

        if (msg == null || msg.getHeader() == null) {
            throw new RuntimeException("The encode message is null.");
        }

        byteBuf.writeInt(msg.getHeader().getMagic());
        byteBuf.writeInt(msg.getHeader().getLength());
        byteBuf.writeLong(msg.getHeader().getSessionID());
        byteBuf.writeByte(msg.getHeader().getType());
        byteBuf.writeByte(msg.getHeader().getVersion());
        byteBuf.writeInt(msg.getHeader().getStatus());

        if (msg.getBody() != null) {
            int lengthPosition = byteBuf.writerIndex();
            byteBuf.writeBytes(LENGTH_PLACEHOLDER);
            BodyBuffer byteBuffer = (BodyBuffer) msg.getBody();
            byte[] content = byteBuffer.read();
            byteBuf.writeBytes(content);
            byteBuf.setInt(lengthPosition, byteBuf.writerIndex() - lengthPosition - 4);
        } else {
            byteBuf.writeInt(0);
        }

        byteBuf.setInt(4, byteBuf.readableBytes());
    }
}
