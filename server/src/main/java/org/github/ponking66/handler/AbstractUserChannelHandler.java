package org.github.ponking66.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.common.ProxyConfig;
import org.github.ponking66.core.ProxyChannelManager;
import org.github.ponking66.common.ClientProxyConfig;
import org.github.ponking66.proto3.ProtoRequestResponseHelper;

import java.net.InetSocketAddress;

/**
 * @author pony
 * @date 2023/5/28
 */
public abstract class AbstractUserChannelHandler<T> extends SimpleChannelInboundHandler<T> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, T msg) throws Exception {
        handleChannelRead(ctx, msg);
    }

    public abstract void handleChannelRead(ChannelHandlerContext ctx, T msg) throws Exception;

    /**
     * 建立用户和代理服务器的channel关系后，通知代理客户端建立和代理服务器的channel，
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        Channel userChannel = ctx.channel();
        // 用户请求的监听端口
        InetSocketAddress localAddress = (InetSocketAddress) userChannel.localAddress();
        // 和代理客户端的channel
        Channel cmdChannel = ProxyChannelManager.getCmdChannel(localAddress.getPort());
        if (cmdChannel == null) {
            // 该端口没有代理客户端，直接断开连接
            ctx.close();
        } else {
            // 用户连接到代理服务器时，设置用户连接不可读，等待代理后端服务器连接成功后再改变为可读状态
            userChannel.config().setOption(ChannelOption.AUTO_READ, false);
            // 内网服务信息 ip:port
            ClientProxyConfig clientProxyConfig = ProxyConfig.getProxyInfo(localAddress.getPort());
            String userId = ProxyChannelManager.newUserId();
            // 给 cmdChannel 添加和客户端连接关系
            ProxyChannelManager.addUserChannelToCmdChannel(cmdChannel, userId, userChannel);
            // 通知代理客户端，可以连接代理端口了
            cmdChannel.writeAndFlush(ProtoRequestResponseHelper.connect(
                    clientProxyConfig.getHost(),
                    clientProxyConfig.getPort(),
                    clientProxyConfig.getType(), userId));
        }
        super.channelActive(ctx);
    }

    /**
     * 平衡用户channel和代理客户端channel的读写速度，防止OOM
     */
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel userChannel = ctx.channel();
        Channel cmdChannel = ProxyChannelManager.getCmdChannel(((InetSocketAddress) userChannel.localAddress()).getPort());
        if (cmdChannel == null) {
            // 该端口还没有代理客户端
            ctx.close();
        } else {
            Channel proxyServerChannel = userChannel.attr(AttrConstants.BIND_CHANNEL).get();
            if (proxyServerChannel != null) {
                boolean writable = userChannel.isWritable();
                proxyServerChannel.config().setOption(ChannelOption.AUTO_READ, writable);
            }
        }
        super.channelWritabilityChanged(ctx);
    }

    /**
     * 发生异常，直接关闭连接
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("exception caught", cause);
        ctx.close();
    }
}
