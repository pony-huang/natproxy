package org.github.ponking66.protoctl;

import lombok.Data;
import lombok.Getter;

/**
 * @author pony
 * @date 2023/4/27
 */
@Data
@Getter
public class NettyMessage {

    private Header header;

    private Object body;
}
