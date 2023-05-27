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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class UserUdpChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    /**
     * 建立用户和代理服务器的channel后，通知代理客户端，建立和代理服务器的channel，
     * 为两个 channel 绑定关系
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel userChannel = ctx.channel();
        // 用户请求的监听端口
        InetSocketAddress localAddress = (InetSocketAddress) userChannel.localAddress();
        // 和代理客户端的channel
        Channel cmdChannel = ProxyChannelManager.getCmdChannel(localAddress.getPort());
        if (cmdChannel == null) {
            // 该端口没有代理客户端，直接断开连接
            ctx.close();
        } else {
            // 用户连接到代理服务器时，设置用户连接不可读，等待代理后端服务器连接成功后再改变为可读状态
            userChannel.config().setOption(ChannelOption.AUTO_READ, false);
            // 内网服务信息 ip:port
            ProxyTunnelInfoReq proxyTunnelInfoReq = ProxyConfig.getProxyInfo(localAddress.getPort());
            String userId = ProxyChannelManager.newUserId();
            proxyTunnelInfoReq.setUserId(userId);
            // 给 cmdChannel 添加和客户端连接关系
            ProxyChannelManager.addUserChannelToCmdChannel(cmdChannel, userId, userChannel);
            // 通知代理客户端，可以连接代理端口了
            NettyMessage proxyMessage = new NettyMessage();
            proxyMessage.setHeader(new Header().setType(MessageType.CONNECT_REQUEST));
            proxyMessage.setBody(proxyTunnelInfoReq);
            cmdChannel.writeAndFlush(proxyMessage);
        }
        super.channelActive(ctx);
    }

    /**
     * 将用户请求的数据，转发给对应的代理客户端
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        Channel userChannel = ctx.channel();
        Channel proxyChannel = userChannel.attr(AttrConstants.BIND_CHANNEL).get();
        if (proxyChannel == null) {
            // 如果没有对应的代理客户端，直接关闭连接
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


    /**
     * 平衡用户channel和代理客户端channel的读写速度，防止OOM
     * <p>
     * 比如：如果对方的读取速度太慢，那么我们的 OutBound缓冲区很快就会堆积大量的数据，造成OOM
     *
     * @see <a href="https://www.cnblogs.com/stateis0/p/9062155.html"/>
     */
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel userChannel = ctx.channel();
        InetSocketAddress sa = (InetSocketAddress) userChannel.localAddress();
        Channel cmdChannel = ProxyChannelManager.getCmdChannel(sa.getPort());
        if (cmdChannel == null) {
            // 该端口还没有代理客户端
            ctx.close();
        } else {
            Channel proxyChannel = userChannel.attr(AttrConstants.BIND_CHANNEL).get();
            if (proxyChannel != null) {
                proxyChannel.config().setOption(ChannelOption.AUTO_READ, userChannel.isWritable());
            }
        }
        super.channelWritabilityChanged(ctx);
    }

    /**
     * 发生异常，直接关闭连接
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("exception caught", cause);
        ctx.close();
    }
}

