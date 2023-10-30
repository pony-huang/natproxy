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
public class TransferReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 42L;

    private String userId;
    private byte[] content;

    private InetSocketAddress remoteAddress;

    public TransferReq(String userId, byte[] content) {
        this.userId = userId;
        this.content = content;
    }


}
