package org.github.ponking66.pojo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * @author pony
 * @date 2023/5/19
 */
@Data
public class TransferResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 42L;
    private String userId;

    private byte[] content;

    private InetSocketAddress remoteAddress;

    public TransferResp(String userId, byte[] content) {
        this.userId = userId;
        this.content = content;
    }
}
