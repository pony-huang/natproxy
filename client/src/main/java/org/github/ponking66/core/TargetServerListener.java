package org.github.ponking66.core;

import io.netty.channel.Channel;

public interface TargetServerListener {

    void listen(Channel cmdChannel ,String userId, String ip , int port);
}
