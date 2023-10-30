package org.github.ponking66.core;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.net.BindException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author pony
 * @date 2023/5/23
 */
public abstract class UserApplication {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 启动器进行绑定 AbstractBootstrap.bind();
     *
     * @param port 绑定端口
     * @return ChannelFuture
     */
    abstract protected ChannelFuture bind(int port);

    /**
     * 获取映射的端口
     *
     * @param clientKey 代理客户端的key
     * @return 映射的端口列表
     */
    abstract protected List<Integer> extranetPort(String clientKey);

    /**
     * 开启所有映射的端口，用于处理用户的请求
     *
     * @param clientKey 代理客户端的key
     */
    public final void start(String clientKey) {
        // 根据clientKey开启 代理指定代理客户端映射的端口
        List<Integer> ports = extranetPort(clientKey);
        for (int port : ports) {
            try {
                start(port);
                LOGGER.debug("Registered user port[{}], clientKey[{}]", port, clientKey);
            } catch (Exception ex) {
                // 该端口已经绑定过，直接忽略
                // 如果不是 BindException，抛出异常
                if (!(ex.getCause() instanceof BindException)) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    /**
     * 开启映射的端口，用于处理用户的请求
     */
    public final void start(int port) throws ExecutionException, InterruptedException {
        ChannelFuture future = bind(port);
        future.get();
        Channel userChannel = future.channel();
        // 缓存 port:userChannel
        ProxyChannelManager.addBindChannel(port, userChannel);
    }


    /**
     * 重启ClientKey客户端映射的端口，用于处理用户的请求
     *
     * @param clientKey 代理客户端的key
     */
    public final void restart(String clientKey) {
        // 根据clientKey开启 代理指定代理客户端映射的端口
        start(clientKey);
    }
}
