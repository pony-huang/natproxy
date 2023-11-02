package org.github.ponking66.core;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.common.ProxyConfig;
import org.github.ponking66.pojo.ProxyTunnelInfoReq;


import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 代理连接服务管理器
 * <p>
 * 代理客户端的连接
 * 代理用户的连接
 *
 * @author pony
 * @date 2023/5/23
 */
public class ProxyChannelManager {

       private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 用户唯一标识 生成器
     */
    private static final AtomicInteger USER_ID_PRODUCER = new AtomicInteger(0);

    /**
     * 代理服务器和用户连接的 channel：
     * key：userId
     * value：userChannel
     */
    public static final AttributeKey<Map<String, Channel>> USER_CHANNELS = AttributeKey.newInstance("user_channels");

    /**
     * 目标服务器ip和端口信息，ip:port
     */
    public static final AttributeKey<ProxyTunnelInfoReq> TARGET_SERVER_INFO = AttributeKey.newInstance("target_server_info");

    /**
     * cmdChannel 对应的端口列表
     */
    public static final AttributeKey<List<Integer>> CHANNEL_PORT = AttributeKey.newInstance("channel_port");

    /**
     * 代理客户端的秘钥（clientKey）
     */
    public static final AttributeKey<String> CHANNEL_CLIENT_KEY = AttributeKey.newInstance("channel_client_key");

    /**
     * 每个代理服务器开放的端口，都映射一个 cmdChannel
     * <p>
     * key：代理服务器开放的端口
     * value：控制代理客户端和代理服务器的 channel
     */
    private static final Map<Integer, Channel> portCmdChannelMapping = new ConcurrentHashMap<>();

    /**
     * 每个代理服务器开放的端口，都映射一个 bindChannel
     */
    private static final Map<Integer, Channel> PORT_BIND_CHANNEL_MAPPING = new ConcurrentHashMap<>();

    /**
     * 每个代理客户端的 唯一标示（clientKey），都映射一个 cmdChannel
     * <p>
     * key：代理客户端的 唯一标示（clientKey）
     * value：控制代理客户端和代理服务器的 channel
     */
    private static final Map<String, Channel> cmdChannels = new ConcurrentHashMap<>();

    /**
     * 生成用户连接的唯一标示
     */
    public static String newUserId() {
        return String.valueOf(USER_ID_PRODUCER.incrementAndGet());
    }

    /**
     * 获取 代理客户端和代理服务器 的 cmdChannel
     *
     * @param port 代理服务器的端口
     * @return 控制代客户端和代理服务器的channel（cmdChannel）
     */
    public static Channel getCmdChannel(Integer port) {
        return portCmdChannelMapping.get(port);
    }

    /**
     * 获取 代理客户端和代理服务器 的 cmdChannel
     *
     * @param clientKey 代理客户端的唯一标识
     * @return 控制代客户端和代理服务器的channel（cmdChannel）
     */
    public static Channel getCmdChannel(String clientKey) {
        return cmdChannels.get(clientKey);
    }

    /**
     * 增加 用户连接与代理客户端连接关系
     *
     * @param cmdChannel  代理客户端连接
     * @param userId      用户唯一标识
     * @param userChannel 用户请求channel
     */
    public static void addUserChannelToCmdChannel(Channel cmdChannel, String userId, Channel userChannel) {
        InetSocketAddress sa = (InetSocketAddress) userChannel.localAddress();
        ProxyTunnelInfoReq proxyTunnelInfoReq = ProxyConfig.getProxyInfo(sa.getPort());
        // 绑定关系
        userChannel.attr(AttrConstants.USER_ID).set(userId);
        userChannel.attr(TARGET_SERVER_INFO).set(proxyTunnelInfoReq);
        cmdChannel.attr(USER_CHANNELS).get().put(userId, userChannel);
    }

    /**
     * 获取用户唯一标识
     *
     * @param userChannel 用户连接
     * @return 用户唯一标识
     */
    public static String getUserChannelUserId(Channel userChannel) {
        return userChannel.attr(AttrConstants.USER_ID).get();
    }

    /**
     * 根据代理客户端连接与用户编号获取用户连接
     *
     * @param cmdChannel cmdChannel
     * @param userId     请求用户唯一标识
     * @return userChannel
     */
    public static Channel getUserChannel(Channel cmdChannel, String userId) {
        return cmdChannel.attr(USER_CHANNELS).get().get(userId);
    }

    /**
     * 获取 bindChannel
     *
     * @param port bindChannel 对应的 port
     * @return bindChannel
     */
    public static Channel getBindChannel(Integer port) {
        return PORT_BIND_CHANNEL_MAPPING.get(port);
    }

    public static void addBindChannel(Integer port, Channel bindChannel) {
        PORT_BIND_CHANNEL_MAPPING.put(port, bindChannel);
    }

    public static Channel removeBindChannel(Integer port) {
        return PORT_BIND_CHANNEL_MAPPING.remove(port);
    }

    /**
     * 删除用户连接与代理客户端连接关系
     *
     * @param cmdChannel 代理客户端连接
     * @param userId     用户连接
     * @return 用户连接
     */
    public static Channel removeUserChannelFromCmdChannel(Channel cmdChannel, String userId) {
        if (cmdChannel.attr(USER_CHANNELS).get() == null) {
            return null;
        }
        synchronized (cmdChannel) {
            Map<String, Channel> channelMap = cmdChannel.attr(USER_CHANNELS).get();
            return channelMap.remove(userId);
        }
    }

    /**
     * 代理客户端连接断开后，清除 cmdChannel
     *
     * @param channel cmdChannel
     */
    public static void removeCmdChannel(Channel channel) {
        LOGGER.warn("Proxy channel closed, clear user channels, {}", channel);
        // 如果 cmdChannel 没有开放任何端口
        if (channel.attr(CHANNEL_PORT).get() == null) {
            return;
        }

        String clientKey = channel.attr(CHANNEL_CLIENT_KEY).get();
        // 移除缓存的 cmdChannel
        Channel cacheCmdChannel = cmdChannels.remove(clientKey);
        if (channel != cacheCmdChannel) {
            cmdChannels.put(clientKey, channel);
        }

        // 移除缓存的 cmdChannel
        List<Integer> ports = channel.attr(CHANNEL_PORT).get();
        for (int port : ports) {
            Channel cmdChannel = portCmdChannelMapping.remove(port);
            // 在执行断连之前新的连接已经连上来了
            if (cmdChannel != channel) {
                portCmdChannelMapping.put(port, cmdChannel);
            }
        }

        // close cmdChannel
        if (channel.isActive()) {
            LOGGER.info("disconnect cmd channel {}", channel);
            channel.close();
        }

        // 关闭与cmdChannel关联的所有userChannel
        Map<String, Channel> userChannels = getUserChannels(channel);
        for (String s : userChannels.keySet()) {
            Channel userChannel = userChannels.get(s);
            if (userChannel.isActive()) {
                userChannel.close();
                LOGGER.info("Disconnect user channel {}", userChannel);
            }
        }

        // 释放 cmdChannel 开放的所有端口
        for (int port : ports) {
            Channel bindChannel = removeBindChannel(port);
            if (bindChannel != null) {
                bindChannel.close();
            }
        }
    }

    /**
     * 增加代理服务器端口与代理控制客户端连接的映射关系
     *
     * @param ports      端口映射列表
     * @param clientKey  代理客户端的秘钥
     * @param cmdChannel 控制连接的channel（cmdChannel）
     */
    public static void addCmdChannel(List<Integer> ports, String clientKey, Channel cmdChannel) {
        if (ports == null) {
            throw new IllegalArgumentException("port can not be null");
        }
        // 保证服务器对外端口与客户端到服务器的连接关系在临界情况时调用removeChannel(Channel channel)时不出问题
        // ConcurrentHashMap的线程安全指的是，它的每个方法单独调用（即原子操作）都是线程安全的
        // 如果有两个线程分别调用put和remove方法，就无法保证线程安全了
        synchronized (portCmdChannelMapping) {
            for (int port : ports) {
                portCmdChannelMapping.put(port, cmdChannel);
            }
        }
        cmdChannel.attr(CHANNEL_PORT).set(ports);
        cmdChannel.attr(CHANNEL_CLIENT_KEY).set(clientKey);
        cmdChannel.attr(USER_CHANNELS).set(new ConcurrentHashMap<>());
        // 缓存 cmdChannel
        cmdChannels.put(clientKey, cmdChannel);
    }

    /**
     * 获取代理控制客户端连接绑定的所有用户连接
     *
     * @param cmdChannel 控制连接的channel（cmdChannel）
     * @return userChannels
     */
    public static Map<String, Channel> getUserChannels(Channel cmdChannel) {
        return cmdChannel.attr(USER_CHANNELS).get();
    }

    /**
     * 清理绑定关系
     *
     * @param proxyChannel 代理channel
     */
    public static void unbind(Channel proxyChannel) {
        proxyChannel.attr(AttrConstants.BIND_CHANNEL).set(null);
        proxyChannel.attr(AttrConstants.CLIENT_KEY).set(null);
        proxyChannel.attr(AttrConstants.USER_ID).set(null);
    }

    /**
     * 绑定proxyChannel和userChannel的关系
     *
     * @param token        client_key
     * @param userId       userId
     * @param userChannel  用户channel
     * @param proxyChannel 代理客户端channel
     */
    public static void bind(String token, String userId, Channel userChannel, Channel proxyChannel) {
        proxyChannel.attr(AttrConstants.USER_ID).set(userId);
        proxyChannel.attr(AttrConstants.CLIENT_KEY).set(token);
        proxyChannel.attr(AttrConstants.BIND_CHANNEL).set(userChannel);
        userChannel.attr(AttrConstants.BIND_CHANNEL).set(proxyChannel);
    }

}
