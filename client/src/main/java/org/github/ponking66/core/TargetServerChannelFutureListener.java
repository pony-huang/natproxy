package org.github.ponking66.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.common.ProxyConfig;
import org.github.ponking66.proto3.NatProxyProtos;
import org.github.ponking66.proto3.ProtoRequestResponseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * @author pony
 * @date 2023/5/26
 */
public class TargetServerChannelFutureListener implements ChannelFutureListener {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final Channel cmdChannel;
    private final String userId;
    protected final Bootstrap proxyServerBootstrap;

    public TargetServerChannelFutureListener(Channel cmdChannel, String userId, Bootstrap proxyServerBootstrap) {
        this.cmdChannel = cmdChannel;
        this.userId = userId;
        this.proxyServerBootstrap = proxyServerBootstrap;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        // 通道建立成功
        if (future.isSuccess()) {
            final Channel targetServerChannel = future.channel();
            LOGGER.debug("The target server is successfully connected, channel: {}", targetServerChannel);
            // 由于和代理服务器的通道还未打通，所以先注销掉和目标服务器通道的读事件,避免读缓冲区内存占用过多
            targetServerChannel.config().setOption(ChannelOption.AUTO_READ, false);
            // 建立和代理服务器的通道，同时绑定两个通道的关系
            bind(targetServerChannel, cmdChannel);
        } else {
            // 初始登录失败通知代理服务器关闭对应关系channel
            cmdChannel.writeAndFlush(ProtoRequestResponseHelper.disconnect(null));
        }
    }

    private void bind(Channel targetServerChannel, Channel cmdChannel) {
        // 建立隧道 proxyServerChannel
        proxyServerBootstrap.connect(ProxyConfig.getServerHost(), ProxyConfig.getServerPort())
                .addListener((ChannelFutureListener) future -> {
                    final Channel proxyServerChannel = future.channel();
                    if (future.isSuccess()) {
                        // 绑定
                        borrowProxyChanel(proxyServerBootstrap,
                                bindTargetChannelAndProxyChannel(targetServerChannel, proxyServerChannel),
                                connectFailed(cmdChannel));
                    } else {
                        LOGGER.warn("Failed to connect proxy server, cause: ", future.cause());
                        // 通知代理服务器关闭此代理服务（关闭端口监听）
                        NatProxyProtos.Packet nettyMessage = ProtoRequestResponseHelper.disconnect(userId);
                        proxyServerChannel.writeAndFlush(nettyMessage);
                        cmdChannel.writeAndFlush(nettyMessage);
                    }
                });
    }

    private Consumer<Channel> connectFailed(Channel cmdChannel) {
        return channel -> {
            cmdChannel.writeAndFlush(ProtoRequestResponseHelper.disconnect(userId));
        };
    }

    private Consumer<Channel> bindTargetChannelAndProxyChannel(Channel targetServerChannel, Channel proxyServerChannel) {
        return channel -> {
            LOGGER.info("The proxy server is successfully connected, channel: {}", channel);
            // 两个通道绑定关系
            proxyServerChannel.attr(AttrConstants.BIND_CHANNEL).set(targetServerChannel);
            targetServerChannel.attr(AttrConstants.BIND_CHANNEL).set(proxyServerChannel);
            // 通知代理服务器,代理隧道已经打通
            proxyServerChannel.writeAndFlush(ProtoRequestResponseHelper.proxyTunnelInfoResponse(userId));
            // 重新注册和目标服务器通道的读事件,可以进行数据代理传输
            targetServerChannel.config().setOption(ChannelOption.AUTO_READ, true);
            // 建立绑定关系，唯一标示缓存通道
            ClientChannelManager.addTargetServerChannel(userId, targetServerChannel);
            ClientChannelManager.setTargetServerChannelUserId(targetServerChannel, userId);
        };
    }


    /**
     * 代理客户端和目标服务器建立通道后调用此方法
     * 1. 开启代理客户端和代理服务端映射隧道；
     * 2. 绑定目标服务器和代理服务器的的映射关系
     *
     * @param proxyServerBootstrap 建立代理客户端和代理服务器连接的启动器
     * @param successListener      建立隧道成功的监听器
     * @param failedListener       建立隧道失败的监听器
     */
    public void borrowProxyChanel(Bootstrap proxyServerBootstrap, Consumer<Channel> successListener, Consumer<Channel> failedListener) {
        // 返回并移除队列队头的channel
        Channel channel = ClientChannelManager.poolChannel();
        // 若存在可用的channel
        if (channel != null) {
            // 绑定隧道映射关系
            // 目标服务器 <---> 代理客户端；代理客户端 <---> 代理服务器
            successListener.accept(channel);
        } else {
            // 建立隧道
            proxyServerBootstrap
                    .connect(ProxyConfig.getServerHost(), ProxyConfig.getServerPort())
                    .addListener((ChannelFutureListener) future -> {
                        // 连接成功
                        if (future.isSuccess()) {
                            // 绑定关系
                            successListener.accept(future.channel());
                        } else {
                            // 通知代理服务器关闭此代理服务（关闭端口监听）
                            failedListener.accept(future.channel());
                        }
                    });
        }
    }
}
