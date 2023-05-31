package org.github.ponking66.pojo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author pony
 * @date 2023/5/31
 */
@Data
public class LoginResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 42L;

    private RespError error;

    public LoginResp(RespError error) {
        this.error = error;
    }

    public static enum RespError {

        /**
         * 已登录
         */
        LOGGED,

        /**
         * client key error
         */
        CLIENT_KEY_ERROR;

    }
}
