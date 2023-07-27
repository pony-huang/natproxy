package org.github.ponking66.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import org.github.ponking66.core.ProxyChannelManager;
import org.github.ponking66.proto3.NatProxyProtos;
import org.github.ponking66.util.ObjectUtils;

/**
 * 处理代理客户端连接请求
 *
 * @author pony
 * @date 2023/4/28
 */
public class ServerTunnelConnectHandler extends ProtoHandler {

    @Override
    public void handleRead(ChannelHandlerContext ctx, NatProxyProtos.Packet packet) {
        // 如果msg.getBody() == null ，关闭连接
        NatProxyProtos.ProxyTunnelInfoResponse response = packet.getProxyTunnelInfoResponse();
        if (ObjectUtils.isEmpty(response.getToken())) {
            ctx.close();
            return;
        }

        String token = response.getToken();
        String userId = response.getUserId();

        Channel cmdChannel = ProxyChannelManager.getCmdChannel(token);
        // clientKey 错误，关闭连接
        if (cmdChannel == null) {
            LOGGER.warn("Connect message error, clientKey: {}", token);
            ctx.close();
            return;
        }

        Channel userChannel = ProxyChannelManager.getUserChannel(cmdChannel, userId);
        if (userChannel == null) {
            LOGGER.warn("Not exist userChannel, ProxyTunnelInfoResp: {}", response);
            ctx.close();
            return;
        }
        // 绑定proxyChannel和userChannel的关系
        Channel proxyChannel = ctx.channel();
        ProxyChannelManager.bind(token, userId, userChannel, proxyChannel);
        // 代理客户端与后端服务器连接成功，修改用户连接为可读状态
        userChannel.config().setOption(ChannelOption.AUTO_READ, true);
    }

    @Override
    public NatProxyProtos.Header.MessageType getMessageType() {
        return NatProxyProtos.Header.MessageType.CONNECT_RESPONSE;
    }

}
