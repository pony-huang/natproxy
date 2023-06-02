package org.github.ponking66.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.common.ProxyConfig;
import org.github.ponking66.core.ProxyChannelManager;
import org.github.ponking66.pojo.ProxyTunnelInfoReq;
import org.github.ponking66.pojo.TransferRep;
import org.github.ponking66.protoctl.Header;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;


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
public class UserUdpChannelHandler extends AbstractUserChannelHandler<DatagramPacket> {

    @Override
    public void handleChannelRead(ChannelHandlerContext ctx, DatagramPacket msg) {
        Channel userChannel = ctx.channel();
        Channel proxyChannel = userChannel.attr(AttrConstants.BIND_CHANNEL).get();
        // 如果没有对应的代理客户端，直接关闭连接
        if (proxyChannel == null) {
            ctx.close();
        } else {
            int size = msg.content().readableBytes();
            byte[] bytes = new byte[size];
            msg.content().readBytes(bytes);
            String userId = ProxyChannelManager.getUserChannelUserId(userChannel);
            NettyMessage proxyMessage = new NettyMessage();
            long sessionID = System.currentTimeMillis();
            InetSocketAddress sender = msg.sender();
            proxyMessage.setHeader(new Header().setSessionID(sessionID).setType(MessageType.TRANSFER_REQUEST));
            TransferRep rep = new TransferRep(userId, bytes);
            rep.setRemoteAddress(sender);
            proxyMessage.setBody(rep);
            proxyChannel.writeAndFlush(proxyMessage);
        }
    }

}

