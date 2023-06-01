
package org.github.ponking66.common;

import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * @author huang
 */
@Data
@ToString
public class Server {

    private String host;
    private int port;
    private List<Proxy> proxy;
    private List<String> keys;
    private TLSConfig tls;

}