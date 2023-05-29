package org.github.ponking66.handler;

import io.netty.buffer.ByteBuf;
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
public class ClientTunnelTransferHandler extends BaseHandler {

    @Override
    public void handleRead(ChannelHandlerContext ctx, NettyMessage message) {
        Channel targetServerChannel = ctx.channel().attr(AttrConstants.BIND_CHANNEL).get();
        if (targetServerChannel != null) {
            if (message.getBody() instanceof TransferRep rep) {

                byte[] data = rep.getContent();
                ByteBuf buf = ctx.alloc().buffer(data.length);
                buf.writeBytes(data);

                if (targetServerChannel instanceof NioDatagramChannel) {

                    InetSocketAddress proxySeverRemoteAddress = rep.getRemoteAddress();
                    InetSocketAddress targetSeverLocalAddress = (InetSocketAddress) targetServerChannel.remoteAddress();
                    ClientChannelManager.bindMappedAddress(targetSeverLocalAddress, proxySeverRemoteAddress);
                    DatagramPacket packet = new DatagramPacket(buf, targetSeverLocalAddress);
                    targetServerChannel.writeAndFlush(packet);

                    LOGGER.debug("Write data to target server. {}", targetServerChannel);
                } else if (targetServerChannel instanceof NioSocketChannel) {
                    targetServerChannel.writeAndFlush(buf);
                    LOGGER.debug("Write data to target server. {}", targetServerChannel);
                } else {
                    LOGGER.warn("Illegal channel, cannot transfer. {}", targetServerChannel);
                }

                LOGGER.debug("Write data to target server. {}", targetServerChannel);
            } else {
                LOGGER.warn("Parameter error. {}", targetServerChannel);
            }
        }
    }

    /**
     * <pre>平衡读写速度，防止内存占用过多，出现OOM。</pre>
     */
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel proxyServerChannel = ctx.channel();
        Channel targetServerChannel = proxyServerChannel.attr(AttrConstants.BIND_CHANNEL).get();
        if (targetServerChannel != null) {
            boolean writable = proxyServerChannel.isWritable();
            LOGGER.debug("ProxyServerChannel is Writable: {}", writable);
            targetServerChannel.config().setOption(ChannelOption.AUTO_READ, writable);
        }
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public byte getMessageType() {
        return MessageType.TRANSFER_REQUEST;
    }
}
