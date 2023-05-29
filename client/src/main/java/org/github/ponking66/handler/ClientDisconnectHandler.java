package org.github.ponking66.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.github.ponking66.ClientApplication;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.core.ClientChannelManager;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;

/**
 * @author pony
 * @date 2023/4/28
 */
public class ClientDisconnectHandler extends BaseHandler {

    private final ClientApplication clientApplication;

    public ClientDisconnectHandler(ClientApplication clientApplication) {
        this.clientApplication = clientApplication;
    }

    @Override
    public void handleRead(ChannelHandlerContext ctx, NettyMessage message) {
        // 1 解除隧道关系绑定
        // 2 返回proxyServerChannel给连接池
        // 3 通知目标服务器关闭 socket，然后 close channel
        Channel proxyServerChannel = ctx.channel();
        Channel targetServerChannel = proxyServerChannel.attr(AttrConstants.BIND_CHANNEL).get();
        if (targetServerChannel != null) {
            // 解除关系绑定
            proxyServerChannel.attr(AttrConstants.BIND_CHANNEL).set(null);
            // 返回连接池
            ClientChannelManager.returnProxyChannel(proxyServerChannel);
            // 通知目标服务器关闭 socket，然后 close channel
            targetServerChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            LOGGER.info("Close the target server channel.");
        }
    }

    @Override
    public byte getMessageType() {
        return MessageType.DISCONNECT;
    }

    /**
     * 销毁控制连接的channel，回收数据连接的channel
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel proxyServerChannel = ctx.channel();
        // 如果控制连接的channel close了
        if (proxyServerChannel == ClientChannelManager.getCmdChannel()) {
            // GC 控制连接的channel
            ClientChannelManager.setCmdChannel(null);
            // 通知所有的真实服务器close socket，关闭所有和真实服务器的channel
            ClientChannelManager.clearTargetServerChannels();
            // 尝试重连代理服务器
            clientApplication.connect();
        } else {
            // 如果是数据传输的channel，则直接关闭
            Channel targetServerChannel = proxyServerChannel.attr(AttrConstants.BIND_CHANNEL).get();
            if (targetServerChannel != null && targetServerChannel.isActive()) {
                targetServerChannel.close();
            }
        }
        // 移除连接池中的channel
        ClientChannelManager.removeProxyChannel(proxyServerChannel);
        super.channelInactive(ctx);
    }

}
