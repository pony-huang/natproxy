package org.github.ponking66.pojo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author pony
 * @date 2023/5/31
 */
@Data
public class LoginReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 42L;

    private String clientKey;

    public LoginReq(String clientKey) {
        this.clientKey = clientKey;
    }
}
