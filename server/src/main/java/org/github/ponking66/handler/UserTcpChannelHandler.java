package org.github.ponking66.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.core.ProxyChannelManager;
import org.github.ponking66.pojo.CloseChannelRep;
import org.github.ponking66.pojo.TransferRep;
import org.github.ponking66.protoctl.Header;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.net.InetSocketAddress;

/**
 * 处理用户的请求
 * <p>
 * 1.处理用户的连接请求
 * 2.转发用户的请求
 * </p>
 *
 * @author pony
 * @date 2023/5/23
 */
public class UserTcpChannelHandler extends AbstractUserChannelHandler<ByteBuf> {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Override
    public void handleChannelRead(ChannelHandlerContext ctx, ByteBuf msg) {
        Channel userChannel = ctx.channel();
        if (userChannel.isActive() && userChannel.isWritable()) {
            Channel proxyChannel = userChannel.attr(AttrConstants.BIND_CHANNEL).get();
            if (proxyChannel == null) {
                // 如果没有对应的代理客户端，直接关闭连接
                ctx.close();
            } else {
                byte[] data = new byte[msg.readableBytes()];
                msg.readBytes(data);
                String userId = ProxyChannelManager.getUserChannelUserId(userChannel);
                NettyMessage proxyMessage = new NettyMessage();
                proxyMessage.setHeader(new Header().setType(MessageType.TRANSFER_REQUEST));
                TransferRep rep = new TransferRep(userId, data);
                proxyMessage.setBody(rep);
                proxyChannel.writeAndFlush(proxyMessage);
            }
        } else {
            LOGGER.error("Message dropped.");
        }
    }

    /**
     * 通知代理客户端断开指定的连接，清理缓存
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel userChannel = ctx.channel();
        InetSocketAddress localAddress = (InetSocketAddress) userChannel.localAddress();
        Channel cmdChannel = ProxyChannelManager.getCmdChannel(localAddress.getPort());
        if (cmdChannel == null) {
            // 如果没有对应的代理客户端，直接关闭连接
            ctx.close();
        } else {
            // 通知代理客户端断开指定的连接，清理缓存
            String userId = ProxyChannelManager.getUserChannelUserId(userChannel);
            ProxyChannelManager.removeUserChannelFromCmdChannel(cmdChannel, userId);
            Channel proxyChannel = userChannel.attr(AttrConstants.BIND_CHANNEL).get();
            if (proxyChannel != null && proxyChannel.isActive()) {
                // 清理绑定关系
                proxyChannel.attr(AttrConstants.BIND_CHANNEL).set(null);
                proxyChannel.attr(AttrConstants.CLIENT_KEY).set(null);
                proxyChannel.attr(AttrConstants.USER_ID).set(null);

                // 设置可读
                proxyChannel.config().setOption(ChannelOption.AUTO_READ, true);
                // 通知客户端，用户连接已经断开
                NettyMessage message = new NettyMessage();
                message.setHeader(new Header().setType(MessageType.DISCONNECT));
                CloseChannelRep rep = new CloseChannelRep(userId, null);
                message.setBody(rep);

                proxyChannel.writeAndFlush(message);
            }
        }
        super.channelInactive(ctx);
    }

}
