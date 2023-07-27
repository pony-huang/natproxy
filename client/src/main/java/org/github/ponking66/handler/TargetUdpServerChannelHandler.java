package org.github.ponking66.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.core.ClientChannelManager;
import org.github.ponking66.proto3.ProtoRequestResponseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;


/**
 * 处理代理客户端和目标服务器（UDP）
 *
 * @author pony
 * @date 2023/4/28
 */
public class TargetUdpServerChannelHandler extends AbstractTargetServerChannelHandler<DatagramPacket> {


    /**
     * 读取目标服务器的消息，写给代理服务器
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        Channel targetServerChannel = ctx.channel();
        Channel proxyServerChannel = targetServerChannel.attr(AttrConstants.BIND_CHANNEL).get();
        // 如果代理客户端和代理服务器已经断开连接
        if (proxyServerChannel == null) {
            // 关闭代理客户端和目标服务器的channel
            targetServerChannel.close();
            return;
        }

        // 目标服务器响应的消息
        DatagramPacket packet = msg.duplicate();
        int size = packet.content().readableBytes();
        byte[] bytes = new byte[size];
        packet.content().readBytes(bytes);

        InetSocketAddress sender = msg.sender();
        InetSocketAddress inetSocketAddress = ClientChannelManager.getMappedAddressProxySeverUserRemoteAddress(sender);
        String userId = ClientChannelManager.getTargetServerChannelUserId(targetServerChannel);

        proxyServerChannel.writeAndFlush(ProtoRequestResponseHelper.transferResponse(bytes, userId, inetSocketAddress));
        LOGGER.debug("Write data to proxy server[UDP]. {} -->> {}", targetServerChannel.remoteAddress(), proxyServerChannel.remoteAddress());
    }


}
