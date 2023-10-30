package org.github.ponking66.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.core.ProxyChannelManager;
import org.github.ponking66.pojo.CloseChannelRep;
import org.github.ponking66.protoctl.Header;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;
import org.github.ponking66.util.RequestResponseUtils;


import java.net.InetSocketAddress;

/**
 * 处理用户的连接请求,转发用户的请求
 *
 * @author pony
 * @date 2023/5/23
 */
public class UserTcpChannelHandler extends AbstractUserChannelHandler<ByteBuf> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handleChannelRead(ChannelHandlerContext ctx, ByteBuf msg) {
        Channel userChannel = ctx.channel();
        if (userChannel.isActive() && userChannel.isWritable()) {
            Channel proxyChannel = userChannel.attr(AttrConstants.BIND_CHANNEL).get();
            // 如果没有对应的代理客户端，直接关闭连接
            if (proxyChannel == null) {
                ctx.close();
                return;
            }
            byte[] data = new byte[msg.readableBytes()];
            msg.readBytes(data);
            String userId = ProxyChannelManager.getUserChannelUserId(userChannel);
            proxyChannel.writeAndFlush(RequestResponseUtils.transferRep(data, userId));
        } else {
            LOGGER.error("Message dropped.");
        }
    }


    /**
     * 通知代理客户端断开指定的连接
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel userChannel = ctx.channel();
        InetSocketAddress localAddress = (InetSocketAddress) userChannel.localAddress();
        Channel cmdChannel = ProxyChannelManager.getCmdChannel(localAddress.getPort());

        // 如果没有对应的代理客户端，直接关闭连接
        if (cmdChannel == null) {
            ctx.close();
            return;
        }

        // 通知代理客户端断开指定的连接，清理缓存
        String userId = ProxyChannelManager.getUserChannelUserId(userChannel);
        ProxyChannelManager.removeUserChannelFromCmdChannel(cmdChannel, userId);
        Channel proxyChannel = userChannel.attr(AttrConstants.BIND_CHANNEL).get();
        if (proxyChannel != null && proxyChannel.isActive()) {
            // 清理绑定关系
            ProxyChannelManager.unbind(proxyChannel);
            // 设置可读
            proxyChannel.config().setOption(ChannelOption.AUTO_READ, true);
            // 通知客户端，用户连接已经断开
            proxyChannel.writeAndFlush(RequestResponseUtils.disconnect(userId));
        }


        super.channelInactive(ctx);
    }
}
