package org.github.ponking66.pojo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * @author pony
 * @date 2023/5/19
 */
@Data
public class CloseChannelReq implements Serializable, BodyBuffer {

    @Serial
    private static final long serialVersionUID = 42L;
    private String userId;
    private String token;

    public CloseChannelReq(String userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    @Override
    public void write(byte[] bytes) {
        ByteBuf byteBuf = Unpooled.copiedBuffer(bytes);
        this.userId = readString(byteBuf);
        this.token = readString(byteBuf);
    }

    @Override
    public byte[] read() {
        ByteBuf buffer = Unpooled.buffer();
        writeString(buffer, userId);
        writeString(buffer, token);
        return buffer.array();
    }


    private void writeString(ByteBuf byteBuf, String content) {
        if (content == null || content.isEmpty()) {
            byteBuf.writeInt(0);
        } else {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            byteBuf.writeInt(bytes.length);
            byteBuf.writeBytes(bytes);
        }
    }

    private String readString(ByteBuf byteBuf) {
        int length = byteBuf.readInt();
        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}

