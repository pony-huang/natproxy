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
import org.github.ponking66.core.ClientChannelManager;
import org.github.ponking66.pojo.TransferRep;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;

import java.net.InetSocketAddress;

/**
 * @author pony
 * @date 2023/5/18
 */
public class ClientTunnelTransferHandler extends Handler {

    @Override
    public void handleRead(ChannelHandlerContext ctx, NettyMessage message) {
        Channel targetServerChannel = ctx.channel().attr(AttrConstants.BIND_CHANNEL).get();
        if (targetServerChannel == null) {
            LOGGER.warn("TargetServerChannel is null");
            return;
        }

        TransferRep rep = (TransferRep) message.getBody();
        if (targetServerChannel instanceof NioDatagramChannel) {

            byte[] data = rep.getContent();
            ByteBuf buf = Unpooled.buffer(data.length);
            buf.writeBytes(data);
            InetSocketAddress proxySeverRemoteAddress = rep.getRemoteAddress();
            InetSocketAddress targetSeverLocalAddress = (InetSocketAddress) targetServerChannel.remoteAddress();
            ClientChannelManager.bindMappedAddress(targetSeverLocalAddress, proxySeverRemoteAddress);
            DatagramPacket packet = new DatagramPacket(buf, targetSeverLocalAddress);
            targetServerChannel.writeAndFlush(packet);
            LOGGER.debug("Write data to target server, {}", targetServerChannel);

        } else if (targetServerChannel instanceof NioSocketChannel) {

            byte[] data = rep.getContent();
            ByteBuf buf = Unpooled.buffer(data.length);
            buf.writeBytes(data);
            targetServerChannel.writeAndFlush(buf);
            LOGGER.debug("Write data to target server, {}", targetServerChannel);

        } else {
            LOGGER.warn("Illegal channel, cannot transfer, {}", targetServerChannel);
        }

    }


    /**
     * 平衡读写速度
     */
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel proxyServerChannel = ctx.channel();
        Channel targetServerChannel = proxyServerChannel.attr(AttrConstants.BIND_CHANNEL).get();
        if (targetServerChannel != null) {
            boolean writable = proxyServerChannel.isWritable();
            targetServerChannel.config().setOption(ChannelOption.AUTO_READ, writable);
        }
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public byte getMessageType() {
        return MessageType.TRANSFER_REQUEST;
    }
}
