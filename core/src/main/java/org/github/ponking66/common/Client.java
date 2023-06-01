
package org.github.ponking66.common;

import lombok.Data;
import lombok.ToString;

/**
 * @author huang
 */
@Data
@ToString
public class Client {

    private String key;

    private TLSConfig tls;

}