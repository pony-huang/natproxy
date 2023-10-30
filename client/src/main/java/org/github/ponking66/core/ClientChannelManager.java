package org.github.ponking66.core;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.github.ponking66.common.AttrConstants;



import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 连接管理器
 * <p>
 * * 代理客户端<--->代理服务器
 * * 目标服务器<--->代理客户端
 * </p>
 *
 * @author pony
 * @date 2023/5/22
 */
public class ClientChannelManager {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 和代理服务器的连接池的Channel的最大数量
     */
    private static final int MAX_POOL_SIZE = 10;

    /**
     * 和代理服务器的Channel的连接池
     */
    private static final ConcurrentLinkedQueue<Channel> PROXY_CHANNEL_POOL = new ConcurrentLinkedQueue<Channel>();

    /**
     * 和目标服务器的 Channel 集合
     * key：userId
     * value：Channel
     */
    private static final Map<String, Channel> TARGET_SERVER_CHANNELS = new ConcurrentHashMap<>();

    /**
     * 获取 代理服务器端注册的隧道映射关系的channel
     */
    private static volatile Channel cmdChannel;

    public static synchronized Channel getCmdChannel() {
        return cmdChannel;
    }

    public static synchronized void setCmdChannel(Channel cmdChannel) {
        ClientChannelManager.cmdChannel = cmdChannel;
    }


    public static Channel poolChannel() {
        return PROXY_CHANNEL_POOL.poll();
    }

    /**
     * 解除proxyChannel和目标服务器的关系，归还channel给代理连接池
     *
     * @param proxyChannel proxyChannel
     */
    public static void returnProxyChannel(Channel proxyChannel) {
        // 如果超过最大的连接数量，直接close
        if (PROXY_CHANNEL_POOL.size() > MAX_POOL_SIZE) {
            LOGGER.debug("Pool limit,close the proxy server channel.");
            proxyChannel.close();
        } else {
            proxyChannel.config().setOption(ChannelOption.AUTO_READ, true);
            // 解除proxyChannel和目标服务器的关系
            proxyChannel.attr(AttrConstants.BIND_CHANNEL).set(null);
            // 添加到连接池队尾
            PROXY_CHANNEL_POOL.offer(proxyChannel);
            LOGGER.debug("Return ProxyChanel in the pool, channel is {}, pool current size is {} ", proxyChannel, PROXY_CHANNEL_POOL.size());
        }
    }

    /**
     * 移除连接池中指定的 proxyChannel
     *
     * @param proxyChannel proxyChannel
     */
    public static void removeProxyChannel(Channel proxyChannel) {
        PROXY_CHANNEL_POOL.remove(proxyChannel);
    }

    /**
     * 设置 代理客户端和目标服务器的channel的唯一标示
     *
     * @param targetServerChannel 代理客户端和目标服务器的channel
     * @param userId              唯一标识
     */
    public static void setTargetServerChannelUserId(Channel targetServerChannel, String userId) {
        targetServerChannel.attr(AttrConstants.USER_ID).set(userId);
    }

    /**
     * 获取 代理客户端和目标服务器的channel的唯一标示
     *
     * @param targetServerChannel 代理客户端和目标服务器的channel
     * @return channel的唯一标示
     */
    public static String getTargetServerChannelUserId(Channel targetServerChannel) {
        return targetServerChannel.attr(AttrConstants.USER_ID).get();
    }

    /**
     * 根据指定 的 channel 唯一标示 userId 获取 channel
     *
     * @param userId targetServerChannel 唯一标识
     * @return targetServerChannel
     */
    public static Channel getTargetServerChannel(String userId) {
        return TARGET_SERVER_CHANNELS.get(userId);
    }

    /**
     * 添加 channel
     *
     * @param userId              channel 唯一标示
     * @param targetServerChannel targetServerChannel
     */
    public static void addTargetServerChannel(String userId, Channel targetServerChannel) {
        TARGET_SERVER_CHANNELS.put(userId, targetServerChannel);
    }

    /**
     * 移除指定的 TargetServerChannel，并返回
     *
     * @param userId TargetServerChannel 唯一标识
     * @return TargetServerChannel
     */
    public static Channel removeTargetServerChannel(String userId) {
        return TARGET_SERVER_CHANNELS.remove(userId);
    }

    /**
     * 移除所有的 TargetServerChannel，如果 TargetServerChannel 没有close，通知真实服务端关闭socket
     */
    public static void clearTargetServerChannels() {
        LOGGER.warn("channel closed, clear Target server channels");

        for (Map.Entry<String, Channel> targetChannel : TARGET_SERVER_CHANNELS.entrySet()) {
            Channel TargetServerChannel = targetChannel.getValue();
            if (TargetServerChannel.isActive()) {
                // 通知目标服务端关闭socket
                TargetServerChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }
        // 清空连接池
        TARGET_SERVER_CHANNELS.clear();
    }


    public static final Map<InetSocketAddress, InetSocketAddress> UDP_TUNNEL_BIND = new ConcurrentHashMap<>();


    public static void bindMappedAddress(InetSocketAddress targetSeverLocalAddress, InetSocketAddress proxySeverRemoteAddress) {
        UDP_TUNNEL_BIND.put(targetSeverLocalAddress, proxySeverRemoteAddress);
    }

    public static InetSocketAddress getMappedAddressProxySeverUserRemoteAddress(InetSocketAddress targetSeverLocalAddress) {
        return UDP_TUNNEL_BIND.get(targetSeverLocalAddress);
    }

    public static void removeMappedAddress(InetSocketAddress targetSeverLocalAddress) {
        UDP_TUNNEL_BIND.remove(targetSeverLocalAddress);
    }

}
