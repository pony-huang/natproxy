package org.github.ponking66.handler;

import io.netty.channel.Channel;
import org.github.ponking66.protoctl.NettyMessage;

public interface TunnelResultHandler {

    void success(Channel ctx, NettyMessage msg);

    void error(Channel ctx, Throwable throwable);
}
