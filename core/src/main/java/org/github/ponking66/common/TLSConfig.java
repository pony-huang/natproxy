package org.github.ponking66.common;

import lombok.Data;
import lombok.ToString;

/**
 * @author pony
 * @date 2023/6/1
 */
@Data
@ToString
public class TLSConfig {

    private boolean enable = true;

    public String keyCertChainFile;

    public String keyFile;

    public String caFile;

    public int port;

}
