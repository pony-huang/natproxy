package org.github.ponking66.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.core.ClientChannelManager;
import org.github.ponking66.proto3.NatProxyProtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pony
 * @date 2023/4/28
 */
public class ClientDisconnectHandler extends ProtoHandler {



    @Override
    public void handleRead(ChannelHandlerContext ctx, NatProxyProtos.Packet packet) {
        Channel proxyServerChannel = ctx.channel();
        Channel targetServerChannel = proxyServerChannel.attr(AttrConstants.BIND_CHANNEL).get();
        if (targetServerChannel != null) {
            // /解除隧道关系绑定
            proxyServerChannel.attr(AttrConstants.BIND_CHANNEL).set(null);
            // 清空 ProxyServerChannel
            ClientChannelManager.returnProxyChannel(proxyServerChannel);
            // 通知目标服务器关闭socket，并且关闭客户端socket
            targetServerChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            LOGGER.info("Close the target server channel");
        }
    }

    @Override
    public NatProxyProtos.Header.MessageType getMessageType() {
        return NatProxyProtos.Header.MessageType.DISCONNECT;
    }

}
