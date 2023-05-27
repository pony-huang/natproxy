package org.github.ponking66.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import org.github.ponking66.common.AttrConstants;
import org.github.ponking66.pojo.ProxyTunnelInfoResp;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;
import org.github.ponking66.core.ProxyChannelManager;

/**
 * 处理代理客户端连接请求
 *
 * @author pony
 * @date 2023/4/28
 */
public class ServerTunnelConnectHandler extends BaseHandler {

    @Override
    public void handleRead(ChannelHandlerContext ctx, NettyMessage msg) {
        // 如果msg.getBody() == null ，关闭连接
        if (msg.getBody() instanceof ProxyTunnelInfoResp resp) {

            String token = resp.getToken();
            String userId = resp.getUserId();

            Channel cmdChannel = ProxyChannelManager.getCmdChannel(token);
            // clientKey 错误，关闭连接
            if (cmdChannel == null) {
                LOGGER.warn("Connect message error, clientKey: {}", token);
                ctx.close();
                return;
            }

            Channel userChannel = ProxyChannelManager.getUserChannel(cmdChannel, userId);
            if (userChannel == null) {
                LOGGER.warn("Not exist userChannel, ProxyTunnelInfoResp: {}", resp);
                ctx.close();
                return;
            }
            // 绑定proxyChannel和userChannel的关系
            Channel proxyChannel = ctx.channel();
            proxyChannel.attr(AttrConstants.USER_ID).set(userId);
            proxyChannel.attr(AttrConstants.CLIENT_KEY).set(token);
            proxyChannel.attr(AttrConstants.BIND_CHANNEL).set(userChannel);
            userChannel.attr(AttrConstants.BIND_CHANNEL).set(ctx.channel());
            // 代理客户端与后端服务器连接成功，修改用户连接为可读状态
            userChannel.config().setOption(ChannelOption.AUTO_READ, true);
        } else {
            LOGGER.warn("Connect message error. ProxyTunnelInfoResp is null");
            ctx.close();
        }
    }

    @Override
    public byte getMessageType() {
        return MessageType.CONNECT_RESPONSE;
    }

}
