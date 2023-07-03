package org.github.ponking66.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.core.ProxyChannelManager;
import org.github.ponking66.util.RequestResponseUtils;

import java.net.InetSocketAddress;

/**
 * 处理用户的连接请求,转发用户的请求
 *
 * @author pony
 * @date 2023/5/23
 */
public class UserUdpChannelHandler extends AbstractUserChannelHandler<DatagramPacket> {

    @Override
    public void handleChannelRead(ChannelHandlerContext ctx, DatagramPacket msg) {
        Channel userChannel = ctx.channel();
        Channel proxyChannel = userChannel.attr(AttrConstants.BIND_CHANNEL).get();
        // 如果没有对应的代理客户端，直接关闭连接
        if (proxyChannel == null) {
            ctx.close();
            return;
        }
        int size = msg.content().readableBytes();
        byte[] bytes = new byte[size];
        msg.content().readBytes(bytes);
        String userId = ProxyChannelManager.getUserChannelUserId(userChannel);
        InetSocketAddress sender = msg.sender();
        proxyChannel.writeAndFlush(RequestResponseUtils.transferRep(bytes, userId, sender));
    }


}

