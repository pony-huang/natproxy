
package org.github.ponking66.common;

import lombok.Data;
import lombok.ToString;

/**
 * @author huang
 */
@Data
@ToString
public class Proxy {

    private String host;

    private int port;

    private String key;

    private String type;

    private int intranetPort;

    private int extranetPort;

}