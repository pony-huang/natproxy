package org.github.ponking66.core;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.BindException;
import java.util.List;

/**
 * @author pony
 * @date 2023/5/23
 */
public abstract class UsersApplication {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());


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
                ChannelFuture future = bind(port);
                future.get();
                Channel bindChannel = future.channel();
                // 缓存 port:bindChannel
                ProxyChannelManager.addBindChannel(port, bindChannel);
                LOGGER.info("Bind user port {}, clientKey {}", port, clientKey);
            } catch (Exception ex) {
                // 该端口已经绑定过，直接忽略
                // 如果不是 BindException，抛出异常
                if (!(ex.getCause() instanceof BindException)) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }
}
