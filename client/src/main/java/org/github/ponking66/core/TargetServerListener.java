package org.github.ponking66.core;

import io.netty.channel.Channel;
import org.github.ponking66.pojo.ProxyTunnelInfoReq;

public interface TargetServerListener {

    void listen(Channel cmdChannel, ProxyTunnelInfoReq message);
}
