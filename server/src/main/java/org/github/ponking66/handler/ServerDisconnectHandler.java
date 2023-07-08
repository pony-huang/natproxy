package org.github.ponking66.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.core.ProxyChannelManager;
import org.github.ponking66.pojo.CloseChannelRep;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;

import java.util.Optional;

/**
 * @author pony
 * @date 2023/4/28
 */
public class ServerDisconnectHandler extends Handler {

    /**
     * 处理代理客户端的断开连接请求
     */
    @Override
    public void handleRead(ChannelHandlerContext ctx, NettyMessage msg) {
        Channel currentChannel = ctx.channel();
        String clientKey = currentChannel.attr(AttrConstants.CLIENT_KEY).get();
        // 代理连接没有连上服务器，由cmdChannel通知用户断开连接
        if (clientKey == null) {
            LOGGER.warn("ClientKey is null");
            if (msg.getBody() instanceof CloseChannelRep rep) {
                String userId = rep.getUserId();
                Optional.of(rep.getUserId()).ifPresent(data -> {
                    Channel userChannel = ProxyChannelManager.removeUserChannelFromCmdChannel(currentChannel, userId);
                    if (userChannel != null) {
                        // 数据发送完成后，关闭连接
                        userChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                    }
                });
                return;
            }
        }

        // 检查clientKey
        Channel cmdChannel = ProxyChannelManager.getCmdChannel(clientKey);
        // 错误的clientKey
        if (cmdChannel == null) {
            LOGGER.warn("Connect message error, clientKey: {}", clientKey);
            return;
        }

        // 如果代理客户端关闭或者目标服务器关闭，通知用户连接断开
        Channel userChannel = ProxyChannelManager.removeUserChannelFromCmdChannel(cmdChannel, currentChannel.attr(AttrConstants.USER_ID).get());
        if (userChannel != null) {
            // 解除绑定的关系
            currentChannel.attr(AttrConstants.BIND_CHANNEL).set(null);
            currentChannel.attr(AttrConstants.CLIENT_KEY).set(null);
            currentChannel.attr(AttrConstants.USER_ID).set(null);
            // 数据发送完成后, 关闭连接
            userChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public byte getMessageType() {
        return MessageType.DISCONNECT;
    }

    /**
     * 代理客户端断开连接
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel proxyChannel = ctx.channel();
        Channel userChannel = proxyChannel.attr(AttrConstants.BIND_CHANNEL).get();
        // 若是存在userChannel说明是普通的proxyChannel,否则就是cmdChannel
        if (userChannel != null && userChannel.isActive()) {
            String clientKey = proxyChannel.attr(AttrConstants.CLIENT_KEY).get();
            String userId = proxyChannel.attr(AttrConstants.USER_ID).get();
            Channel cmdChannel = ProxyChannelManager.getCmdChannel(clientKey);
            if (cmdChannel != null) {
                ProxyChannelManager.removeUserChannelFromCmdChannel(cmdChannel, userId);
            } else {
                LOGGER.warn("CmdChannel is null, clientKey: {}", clientKey);
            }
            // 数据发送完成后，关闭连接
            userChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        } else {
            // 直接清除关系,解除cmdChannel绑定的关系,释放端口
            ProxyChannelManager.removeCmdChannel(proxyChannel);
        }
        super.channelInactive(ctx);
    }

}
