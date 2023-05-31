package org.github.ponking66.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.common.ProxyConfig;
import org.github.ponking66.core.ClientChannelManager;
import org.github.ponking66.pojo.CloseChannelRep;
import org.github.ponking66.pojo.TransferResp;
import org.github.ponking66.protoctl.Header;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 *
 * @author pony
 * @date 2023/4/28
 */
public class TargetTcpServerChannelHandler extends AbstractTargetServerChannelHandler<ByteBuf> {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    /**
     * 读取目标服务器的消息，写给代理服务器
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        Channel targetServerChannel = ctx.channel();
        // 与代理服务器的Channel
        Channel proxyServerChannel = targetServerChannel.attr(AttrConstants.BIND_CHANNEL).get();
        // 若是代理客户端和代理服务器都已经断开连接，则关闭目标服务器
        if (proxyServerChannel == null) {
            // 关闭代理客户端和目标服务器的channel
            targetServerChannel.close();
            return;
        }
        // 目标服务器响应的消息
        byte[] data = new byte[msg.readableBytes()];
        msg.readBytes(data);
        String userId = ClientChannelManager.getTargetServerChannelUserId(targetServerChannel);
        NettyMessage message = new NettyMessage();
        message.setHeader(new Header().setType(MessageType.TRANSFER_RESPONSE));
        TransferResp resp = new TransferResp(userId, data);
        message.setBody(resp);
        proxyServerChannel.writeAndFlush(message);
        LOGGER.debug("TCP proxy, write data to proxy server, {} ---> {}", targetServerChannel.remoteAddress(), proxyServerChannel.remoteAddress());
    }

    /**
     * 代理客户端和目标服务器的连接断开时，关闭移除channel，并且通知代理服务器关闭此服务
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel targetServerChannel = ctx.channel();
        String userId = ClientChannelManager.getTargetServerChannelUserId(targetServerChannel);
        // 关闭移除 TargetServerChannel
        ClientChannelManager.removeTargetServerChannel(userId);
        Channel proxyServerChannel = targetServerChannel.attr(AttrConstants.BIND_CHANNEL).get();
        if (proxyServerChannel != null) {
            LOGGER.debug("TargetServerChannel disconnect, {}", targetServerChannel);
            NettyMessage message = new NettyMessage();
            message.setHeader(new Header().setType(MessageType.DISCONNECT));
            CloseChannelRep rep = new CloseChannelRep(userId, ProxyConfig.client().getKey());
            // 通知服务器端关闭指定服务
            proxyServerChannel.writeAndFlush(rep);
        }
        super.channelInactive(ctx);
    }


}
