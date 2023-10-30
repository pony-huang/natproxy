package org.github.ponking66.protoctl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.github.ponking66.util.MarshallerUtils;

/**
 * @author pony
 * @date 2023/4/27
 */
public class NettyMessageDecoder extends LengthFieldBasedFrameDecoder {

        private static final Logger LOGGER = LogManager.getLogger();

    public NettyMessageDecoder() {
        this(1024 * 1024 * 4, 4, 4, -8, 0);
    }

    public NettyMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }
        NettyMessage msg = new NettyMessage();
        Header header = new Header();
        header.setMagic(frame.readInt());
        header.setLength(frame.readInt());
        header.setSessionID(frame.readLong());
        header.setType(frame.readByte());
        header.setVersion(frame.readByte());
        header.setStatus(frame.readInt());

        if (frame.readableBytes() > 4) {
            int objectSize = frame.readInt();
            ByteBuf buf = frame.slice(frame.readerIndex(), objectSize);
            msg.setBody(MarshallerUtils.readObject(buf));
            frame.readerIndex(frame.readerIndex() + objectSize);
        }

        msg.setHeader(header);
        LOGGER.info("Receive Message, type: {}", header.getType());
        // frame.release() 释放存储，防止内存泄露
        frame.release();
        return msg;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }
}
