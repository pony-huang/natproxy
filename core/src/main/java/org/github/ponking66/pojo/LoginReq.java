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
public class LoginReq implements Serializable, BodyBuffer {

    @Serial
    private static final long serialVersionUID = 42L;
    private String clientKey;

    public LoginReq(String clientKey) {
        this.clientKey = clientKey;
    }

    public LoginReq() {
   
    }

    @Override
    public void write(byte[] bytes) {
        ByteBuf byteBuf = Unpooled.copiedBuffer(bytes);
        this.clientKey = readString(byteBuf);
    }

    @Override
    public byte[] read() {
        ByteBuf buffer = Unpooled.buffer();
        writeString(buffer, clientKey);
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
