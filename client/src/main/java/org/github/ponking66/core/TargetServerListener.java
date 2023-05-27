package org.github.ponking66.core;

import io.netty.channel.Channel;
import org.github.ponking66.protoctl.NettyMessage;

public interface TargetServerListener {

    void listen(Channel cmdChannel, NettyMessage message);
}
