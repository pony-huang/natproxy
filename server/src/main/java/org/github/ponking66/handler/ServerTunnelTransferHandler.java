package org.github.ponking66.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.pojo.TransferResp;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;

import java.net.InetSocketAddress;

/**
 * @author pony
 * @date 2023/5/18
 */
public class ServerTunnelTransferHandler extends BaseHandler {

    @Override
    public void handleRead(ChannelHandlerContext ctx, NettyMessage message) {
        Channel userChannel = ctx.channel().attr(AttrConstants.BIND_CHANNEL).get();
        // 如果userChannel已经关闭了，关闭proxyChannel
        if (userChannel == null || !userChannel.isActive()) {
            LOGGER.info("userChannel is close, userChannel=[{}]", userChannel);
            ctx.close();
            return;
        }
        if (message.getBody() instanceof TransferResp resp) {

            byte[] data = resp.getContent();
            ByteBuf buf = ctx.alloc().buffer(data.length);
            buf.writeBytes(data);

            if (userChannel instanceof NioDatagramChannel) {
                InetSocketAddress inetSocketAddress = resp.getRemoteAddress();
                DatagramPacket packet = new DatagramPacket(buf, inetSocketAddress);
                userChannel.writeAndFlush(packet);
            } else if (userChannel instanceof NioSocketChannel) {
                userChannel.writeAndFlush(buf);
            } else {
                LOGGER.warn("Illegal channel, cannot transfer, {}", userChannel);
            }

        } else {
            LOGGER.warn("Parameter error, {}", userChannel);
        }
    }

    /**
     * 平衡读写速度，防止内存占用过多，出现OOM
     * <p>
     * 如果代理服务器出站缓存区数据达到高警戒位，则触发此方法，注销 userChannel的读事件，防止OOM
     */
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel proxyServerChannel = ctx.channel();
        Channel userChannel = proxyServerChannel.attr(AttrConstants.BIND_CHANNEL).get();
        if (userChannel != null) {
            boolean writable = proxyServerChannel.isWritable();
            LOGGER.debug("ProxyServerChannel is Writable: {}", writable);
            userChannel.config().setOption(ChannelOption.AUTO_READ, writable);
        }
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public byte getMessageType() {
        return MessageType.TRANSFER_RESPONSE;
    }
}
