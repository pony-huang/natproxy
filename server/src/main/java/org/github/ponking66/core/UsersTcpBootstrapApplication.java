package org.github.ponking66.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.github.ponking66.common.Proxy;
import org.github.ponking66.handler.UserTcpChannelHandler;
import org.github.ponking66.proto3.ProtocType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author pony
 * @date 2023/5/23
 */
public class UsersTcpBootstrapApplication extends UserApplication {

    private static final Logger LOGGER = LogManager.getLogger();
    private final ServerBootstrap serverBootstrap;

    public UsersTcpBootstrapApplication(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new UserTcpChannelHandler());
                    }
                });
    }

    @Override
    protected ChannelFuture bind(int port) {
        return serverBootstrap.bind(port);
    }

    @Override
    protected List<Integer> extranetPort(String clientKey) {
        return ProxyChannelManagerFactory.getProxyChannelManager().proxies(clientKey).stream().filter(item -> ProtocType.TCP.equals(item.getType())).map(Proxy::getExtranetPort).collect(Collectors.toList());
    }

}
