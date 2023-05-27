package org.github.ponking66.common;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public interface AttrConstants {

    AttributeKey<Channel> BIND_CHANNEL = AttributeKey.newInstance("bind_channel");

    AttributeKey<String> USER_ID = AttributeKey.newInstance("user_id");
    AttributeKey<String> CLIENT_KEY = AttributeKey.newInstance("client_key");

    AttributeKey<Channel> PROXY_BIND_CHANNEL = AttributeKey.newInstance("proxy_bind_channel");
}
