package org.github.ponking66.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.core.ClientChannelManager;
import org.github.ponking66.pojo.TransferResp;
import org.github.ponking66.protoctl.Header;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;


/**
 * 处理代理客户端和目标服务器 msg 的 handler
 *
 * @author pony
 * @date 2023/4/28
 */
public class TargetUdpServerChannelHandler extends AbstractTargetServerChannelHandler<DatagramPacket> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TargetUdpServerChannelHandler.class);

    /**
     * 读取 目标服务器的消息，写给 代理服务器
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        Channel targetServerChannel = ctx.channel();
        // 和 代理服务器的Channel
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

        NettyMessage message = new NettyMessage();
        String userId = ClientChannelManager.getTargetServerChannelUserId(targetServerChannel);
        message.setHeader(new Header().setType(MessageType.TRANSFER_RESPONSE));
        InetSocketAddress sender = msg.sender();
        InetSocketAddress inetSocketAddress = ClientChannelManager.getMappedAddressProxySeverUserRemoteAddress(sender);
        TransferResp resp = new TransferResp(userId, bytes);
        resp.setRemoteAddress(inetSocketAddress);
        message.setBody(resp);

        proxyServerChannel.writeAndFlush(message);
        LOGGER.debug("UDP proxy, write data to proxy server, {}, {}", targetServerChannel, proxyServerChannel);
    }

}
