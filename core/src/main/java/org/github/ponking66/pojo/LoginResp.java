package org.github.ponking66.pojo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * @author pony
 * @date 2023/5/31
 */
@Data
public class LoginResp implements Serializable, BodyBuffer {

    @Serial
    private static final long serialVersionUID = 42L;

    public LoginResp() {
    }

    private String error;

    public LoginResp(String error) {
        this.error = error;
    }

    @Override
    public void write(byte[] bytes) {
        ByteBuf byteBuf = Unpooled.copiedBuffer(bytes);
        this.error = readString(byteBuf);
    }

    @Override
    public byte[] read() {
        ByteBuf buffer = Unpooled.buffer();
        writeString(buffer, error);
        return buffer.array();
    }

    private void writeString(ByteBuf byteBuf, String content) {
        if (content == null || content.length() == 0) {
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
