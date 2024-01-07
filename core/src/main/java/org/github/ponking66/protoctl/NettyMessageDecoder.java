package org.github.ponking66.protoctl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.github.ponking66.pojo.*;

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
            BodyBuffer bodyBuffer = getBufferByte(header);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            bodyBuffer.write(bytes);
            msg.setBody(bodyBuffer);
            frame.readerIndex(frame.readerIndex() + objectSize);
        }

        msg.setHeader(header);
        LOGGER.info("Receive Message, type: {}", header.getType());
        // frame.release() 释放存储，防止内存泄露
        frame.release();
        return msg;
    }

    private static BodyBuffer getBufferByte(Header header) {
        BodyBuffer bodyBuffer;
        if (MessageType.LOGIN_REQUEST == header.getType()) {
            bodyBuffer = new LoginReq();
        } else if (MessageType.LOGIN_RESPONSE == header.getType()) {
            bodyBuffer = new LoginResp();
        } else if (MessageType.CONNECT_REQUEST == header.getType()) {
            bodyBuffer = new ProxyTunnelInfoReq();
        } else if (MessageType.CONNECT_RESPONSE == header.getType()) {
            bodyBuffer = new ProxyTunnelInfoResp();
        } else if (MessageType.TRANSFER_REQUEST == header.getType()) {
            bodyBuffer = new TransferReq();
        } else if (MessageType.TRANSFER_RESPONSE == header.getType()) {
            bodyBuffer = new TransferResp();
        } else if (MessageType.HEARTBEAT_REQUEST == header.getType()) {
            bodyBuffer = new EmptyBodyBuffer();
        } else if (MessageType.HEARTBEAT_RESPONSE == header.getType()) {
            bodyBuffer = new EmptyBodyBuffer();
        } else {
            throw new RuntimeException("Message Type is wrong! Type:" + header.getType());
        }
        return bodyBuffer;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }
}
