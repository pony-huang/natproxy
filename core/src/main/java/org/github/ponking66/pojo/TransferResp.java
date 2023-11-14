package org.github.ponking66.pojo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * @author pony
 * @date 2023/5/19
 */
@Data
public class TransferResp implements Serializable, BodyBuffer {

    @Serial
    private static final long serialVersionUID = 42L;

    private int remotePort;
    private String remoteHost;
    private String userId;
    private byte[] content;

    @Deprecated
    private InetSocketAddress remoteAddress;

    public TransferResp() {
    }

    public TransferResp(String userId, byte[] content) {
        this.userId = userId;
        this.content = content;
    }

    public TransferResp(String userId, String remoteHost, int remotePort, byte[] content) {
        this.userId = userId;
        this.content = content;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
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

    @Override
    public void write(byte[] bytes) {
        ByteBuf byteBuf = Unpooled.copiedBuffer(bytes);
        this.remotePort = byteBuf.readInt();
        this.remoteHost = readString(byteBuf);
        this.userId = readString(byteBuf);

        int length = byteBuf.readInt();
        content = new byte[length];
        byteBuf.readBytes(content);
    }

    @Override
    public byte[] read() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(remotePort);
        writeString(buffer, remoteHost);
        writeString(buffer, userId);
        buffer.writeInt(content.length);
        buffer.writeBytes(content);
        return buffer.array();
    }
}
