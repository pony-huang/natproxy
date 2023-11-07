package org.github.ponking66.common;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author pony
 * @date 2023/5/15
 */
@Data
public class ClientProxyConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 42L;

    private String host;

    private int port;

    private String userId;

    private String token;

    private String type;
}
