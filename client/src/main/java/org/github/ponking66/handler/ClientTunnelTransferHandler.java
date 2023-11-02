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
import org.github.ponking66.proto3.NatProxyProtos;

import java.net.InetSocketAddress;

/**
 * @author pony
 * @date 2023/5/18
 */
public class ClientTunnelTransferHandler extends ProtoHandler {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Override
    public void handleRead(ChannelHandlerContext ctx, NatProxyProtos.Packet packet) {
        Channel targetServerChannel = ctx.channel().attr(AttrConstants.BIND_CHANNEL).get();
        if (targetServerChannel == null) {
            LOGGER.warn("TargetServerChannel is null");
            return;
        }

        NatProxyProtos.TransferRequest request = packet.getTransferRequest();

        if (targetServerChannel instanceof NioDatagramChannel) {

            byte[] data = request.getContent().toByteArray();
            ByteBuf buf = Unpooled.buffer(data.length);
            buf.writeBytes(data);

            InetSocketAddress proxySeverRemoteAddress =  new InetSocketAddress(request.getRemoteAddressHost(),request.getRemoteAddressPort());
            InetSocketAddress targetSeverLocalAddress = (InetSocketAddress) targetServerChannel.remoteAddress();
            ClientChannelManager.bindMappedAddress(targetSeverLocalAddress, proxySeverRemoteAddress);
            targetServerChannel.writeAndFlush(new DatagramPacket(buf, targetSeverLocalAddress));

            LOGGER.debug("Write data to target server, {}", targetServerChannel);

        } else if (targetServerChannel instanceof NioSocketChannel) {

            byte[] data = request.getContent().toByteArray();
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
    public NatProxyProtos.Header.MessageType getMessageType() {
        return NatProxyProtos.Header.MessageType.TRANSFER_REQUEST;
    }
}
