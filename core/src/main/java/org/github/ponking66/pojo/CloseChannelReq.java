package org.github.ponking66.pojo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author pony
 * @date 2023/5/19
 */
@Data
public class CloseChannelReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 42L;

    private String userId;

    private String token;

    public CloseChannelReq(String userId, String token) {
        this.userId = userId;
        this.token = token;
    }
}
