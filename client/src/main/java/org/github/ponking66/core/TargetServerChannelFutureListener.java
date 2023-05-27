package org.github.ponking66.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.common.ProxyConfig;
import org.github.ponking66.handler.TunnelResultHandler;
import org.github.ponking66.pojo.ProxyTunnelInfoResp;
import org.github.ponking66.protoctl.Header;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pony
 * @date 2023/5/26
 */
public class TargetServerChannelFutureListener implements ChannelFutureListener {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final Channel cmdChannel;
    private final String userId;

    /**
     * 代理客户端和代理服务器的 启动器
     */
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
            LOGGER.debug("Connect target Server success, channel: {}", targetServerChannel);
            // 由于和代理服务器的通道还未打通，所以先注销掉和目标服务器通道的读事件
            // 避免读缓冲区内存占用过多
            targetServerChannel.config().setOption(ChannelOption.AUTO_READ, false);
            // 建立和代理服务器的通道，同时绑定两个通道的关系
            bind(targetServerChannel, cmdChannel, userId);
        } else {
            NettyMessage proxyMessage = new NettyMessage();
            proxyMessage.setHeader(new Header().setType(MessageType.DISCONNECT));
            cmdChannel.writeAndFlush(proxyMessage);
        }
    }

    public void bind(Channel targetServerChannel, Channel cmdChannel, String userId) {
        // 建立隧道
        proxyServerBootstrap.connect(ProxyConfig.getServerHost(), ProxyConfig.getServerPort())
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        // 连接成功
                        Channel proxyServerChannel = future.channel();
                        if (future.isSuccess()) {

                            ClientChannelManager.borrowProxyChanel(proxyServerBootstrap, new TunnelResultHandler() {
                                @Override
                                public void success(Channel ctx, NettyMessage msg) {
                                    // 绑定关系
                                    LOGGER.info("Connect proxy server success.");

                                    // 两个通道绑定关系
                                    proxyServerChannel.attr(AttrConstants.BIND_CHANNEL).set(targetServerChannel);
                                    targetServerChannel.attr(AttrConstants.BIND_CHANNEL).set(proxyServerChannel);

                                    // 通知代理服务器，隧道已经打通
                                    ProxyTunnelInfoResp resp = new ProxyTunnelInfoResp();
                                    resp.setUserId(userId);
                                    resp.setToken(ProxyConfig.client().getKey());
                                    NettyMessage proxyMessage = new NettyMessage();
                                    proxyMessage.setHeader(new Header().setType(MessageType.CONNECT_RESPONSE));
                                    proxyMessage.setBody(resp);
                                    proxyServerChannel.writeAndFlush(proxyMessage);

                                    // 重新注册和目标服务器通道的读事件
                                    targetServerChannel.config().setOption(ChannelOption.AUTO_READ, true);
                                    // 根据唯一标示缓存通道
                                    ClientChannelManager.addTargetServerChannel(userId, targetServerChannel);
                                    // 为通道绑定唯一标示
                                    ClientChannelManager.setTargetServerChannelUserId(targetServerChannel, userId);

                                }

                                @Override
                                public void error(Channel ctx, Throwable throwable) {
                                    NettyMessage proxyMessage = new NettyMessage();
                                    proxyMessage.setHeader(new Header().setType(MessageType.DISCONNECT));
                                    proxyMessage.setBody(userId);
                                    cmdChannel.writeAndFlush(proxyMessage);
                                }
                            });

                        } else {
                            LOGGER.warn("Connect proxy server failed.", future.cause());
                            // 通知代理服务器关闭此代理服务（关闭端口监听）
                            NettyMessage proxyMessage = new NettyMessage();
                            proxyMessage.setHeader(new Header().setType(MessageType.DISCONNECT));
                            proxyServerChannel.writeAndFlush(proxyMessage);
                            cmdChannel.writeAndFlush(proxyMessage);
                        }
                    }
                });
    }
}
