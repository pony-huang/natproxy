package org.github.ponking66.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.github.ponking66.common.Proxy;
import org.github.ponking66.common.ProxyConfig;
import org.github.ponking66.handler.UserUdpChannelHandler;
import org.github.ponking66.protoctl.ProtocType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;
import java.util.stream.Collectors;

/**
 * @author pony
 * @date 2023/5/23
 */
public class UsersUdpBootstrapApplication extends UsersApplication {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final Bootstrap bootstrap;

    public UsersUdpBootstrapApplication(EventLoopGroup workerGroup) {
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    public void initChannel(NioDatagramChannel ch) throws Exception {
                        // 转发用户请求
                        ch.pipeline().addLast(new UserUdpChannelHandler());
                    }
                });
    }

    @Override
    protected ChannelFuture bind(int port) {
        return bootstrap.bind(port);
    }

    @Override
    protected List<Integer> extranetPort(String clientKey) {
        return ProxyConfig.server().getProxy().stream().filter(item -> ProtocType.UDP.equals(item.getType())).map(Proxy::getExtranetPort).collect(Collectors.toList());
    }

}
