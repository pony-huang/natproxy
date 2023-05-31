package org.github.ponking66.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.github.ponking66.Application;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.core.ClientChannelManager;

/**
 * @author pony
 * @date 2023/5/31
 */
public class ClientCenterHandler extends ChannelInboundHandlerAdapter {

    private final Application clientApplication;

    public ClientCenterHandler(Application clientApplication) {
        this.clientApplication = clientApplication;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
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
            clientApplication.start();
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
