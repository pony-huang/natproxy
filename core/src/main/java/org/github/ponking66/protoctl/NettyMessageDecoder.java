package org.github.ponking66.protoctl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.jboss.marshalling.ByteInput;
import org.jboss.marshalling.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pony
 * @date 2023/4/27
 */
public class NettyMessageDecoder extends LengthFieldBasedFrameDecoder {


    private final Unmarshaller unmarshaller;

    private final Logger LOGGER = LoggerFactory.getLogger(NettyMessageDecoder.class);

    {
        try {
            this.unmarshaller = MarshallingCodecFactory.buildUnMarshalling();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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
        NettyMessage nettyMessage = new NettyMessage();
        Header header = new Header();
        header.setMagic(frame.readInt());
        header.setLength(frame.readInt());
        header.setSessionID(frame.readLong());
        header.setType(frame.readByte());
        header.setVersion(frame.readByte());
        header.setStatus(frame.readInt());

        int size = frame.readInt();
        if (size > 0) {
            Map<String, Object> attach = new HashMap<>(size);
            int keySize;
            byte[] keyArray;
            for (int i = 0; i < size; i++) {
                keySize = frame.readInt();
                keyArray = new byte[keySize];
                frame.readBytes(keyArray);
                attach.put(new String(keyArray, StandardCharsets.UTF_8), decodeObject(frame));
            }
            header.setExtended(attach);
        }
        if (frame.readableBytes() > 4) {
            nettyMessage.setBody(decodeObject(frame));
        }
        nettyMessage.setHeader(header);
        LOGGER.info("Receive Message, type: {}, sessionID: {}", header.getType(), header.getSessionID());
        // 防止内存泄露
        frame.release();
        return nettyMessage;
    }

    public Object decodeObject(ByteBuf in) throws Exception {
        int objectSize = in.readInt();
        ByteBuf buf = in.slice(in.readerIndex(), objectSize);
        ByteInput input = new ChannelBufferByteInput(buf);
        try {
            unmarshaller.start(input);
            Object obj = unmarshaller.readObject();
            unmarshaller.finish();
            in.readerIndex(in.readerIndex() + objectSize);
            return obj;
        } finally {
            unmarshaller.close();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
//        ctx.flush();
        super.channelReadComplete(ctx);
    }
}
