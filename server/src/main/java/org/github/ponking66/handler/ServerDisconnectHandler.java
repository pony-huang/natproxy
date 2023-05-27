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

/**
 * @author pony
 * @date 2023/4/28
 */
public class ServerDisconnectHandler extends BaseHandler {

    @Override
    public void handleRead(ChannelHandlerContext ctx, NettyMessage msg) {
        String clientKey = ctx.channel().attr(AttrConstants.CLIENT_KEY).get();
        // 代理连接没有连上服务器，由 cmdChannel 通知 用户断开连接
        if (clientKey == null) {
            LOGGER.warn("Client is null");
            if (msg.getBody() instanceof CloseChannelRep rep) {
                String userId = rep.getUserId();
                Channel userChannel = ProxyChannelManager.removeUserChannelFromCmdChannel(ctx.channel(), userId);
                if (userChannel != null) {
                    // 数据发送完成后，关闭连接
                    userChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }
                return;
            }
        }

        // 检查clientKey
        Channel cmdChannel = ProxyChannelManager.getCmdChannel(clientKey);
        // 错误的clientKey
        if (cmdChannel == null) {
            LOGGER.warn("Connect message error, clientKey: {}", clientKey);
            return;
        } else {
            LOGGER.info("Remove cmdChannel, clientKey: {}", clientKey);
            ProxyChannelManager.removeCmdChannel(cmdChannel);
        }

        // 如果代理客户端关闭或者目标服务器关闭，通知用户连接断开
        Channel userChannel = ProxyChannelManager.removeUserChannelFromCmdChannel(cmdChannel, ctx.channel().attr(AttrConstants.USER_ID).get());
        if (userChannel != null) {
            // 解除绑定的关系
            ctx.channel().attr(AttrConstants.BIND_CHANNEL).set(null);
            ctx.channel().attr(AttrConstants.CLIENT_KEY).set(null);
            ctx.channel().attr(AttrConstants.USER_ID).set(null);
            // 数据发送完成后, 关闭连接
            userChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public byte getMessageType() {
        return MessageType.DISCONNECT;
    }

    /**
     * 如果代理客户端断开连接：
     * 1 如果此channel有对应的userChannel；
     * 1 清理和userChannel的关系
     * 2 通知userChannel 可以关闭连接了
     * 3 移除管理器缓存的此channel的引用，便于 GC
     * 2 否则，直接清除关系
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel userChannel = ctx.channel().attr(AttrConstants.BIND_CHANNEL).get();
        // 有 userChannel说明是普通的 proxyChannel
        // 否则，就是 cmdChannel
        if (userChannel != null && userChannel.isActive()) {
            String clientKey = ctx.channel().attr(AttrConstants.CLIENT_KEY).get();
            String userId = ctx.channel().attr(AttrConstants.USER_ID).get();
            Channel cmdChannel = ProxyChannelManager.getCmdChannel(clientKey);
            if (cmdChannel != null) {
                ProxyChannelManager.removeUserChannelFromCmdChannel(cmdChannel, userId);
            } else {
                LOGGER.warn("CmdChannel is null, clientKey: {}", clientKey);
            }
            // 数据发送完成后，关闭连接
            userChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        } else {
            // 解除cmdChannel绑定的关系,释放端口
            ProxyChannelManager.removeCmdChannel(ctx.channel());
        }
        super.channelInactive(ctx);
    }


}
