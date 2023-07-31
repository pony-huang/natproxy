package org.github.ponking66.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.proto3.NatProxyProtos;

import java.net.InetSocketAddress;

/**
 * 代理客户端 -->> 用户客户端
 *
 * @author pony
 * @date 2023/5/18
 */
public class ServerTunnelTransferHandler extends ProtoHandler {

    @Override
    public void handleRead(ChannelHandlerContext ctx, NatProxyProtos.Packet packet) {
        Channel proxyChannel = ctx.channel();

        if (!(proxyChannel.isActive() && proxyChannel.isWritable())) {
            LOGGER.error("Message dropped.");
            return;
        }

        Channel userChannel = proxyChannel.attr(AttrConstants.BIND_CHANNEL).get();
        // 如果userChannel已关闭了，则关闭proxyChannel
        if (userChannel == null || !userChannel.isActive()) {
            LOGGER.info("userChannel is close, userChannel=[{}]", userChannel);
            ctx.close();
            return;
        }


        NatProxyProtos.TransferResponse response = packet.getTransferResponse();
        byte[] data = response.getContent().toByteArray();
        ByteBuf buf = Unpooled.buffer(data.length);
        buf.writeBytes(data);

        if (userChannel instanceof NioDatagramChannel) {
            InetSocketAddress inetSocketAddress = new InetSocketAddress(response.getRemoteAddressHost(), response.getRemoteAddressPort());
            DatagramPacket datagramPacket = new DatagramPacket(buf, inetSocketAddress);
            userChannel.writeAndFlush(datagramPacket);
        } else if (userChannel instanceof NioSocketChannel) {
            userChannel.writeAndFlush(buf);
        } else {
            LOGGER.warn("Illegal channel, cannot transfer, {}", userChannel);
        }

    }

    /**
     * 平衡读写速度
     * <p>
     * 如果代理服务器出站缓存区数据达到高警戒位则触发此方法,注销userChannel的读事件避免缓存大量数据
     */
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel proxyServerChannel = ctx.channel();
        Channel userChannel = proxyServerChannel.attr(AttrConstants.BIND_CHANNEL).get();
        if (userChannel != null) {
            boolean writable = proxyServerChannel.isWritable();
            userChannel.config().setOption(ChannelOption.AUTO_READ, writable);
        }
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public NatProxyProtos.Header.MessageType getMessageType() {
        return NatProxyProtos.Header.MessageType.TRANSFER_RESPONSE;
    }
}
