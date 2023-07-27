package org.github.ponking66.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.github.ponking66.core.TargetServerListener;
import org.github.ponking66.core.TcpTargetSeverListener;
import org.github.ponking66.core.UdpTargetSeverListener;
import org.github.ponking66.pojo.ProxyTunnelInfoReq;
import org.github.ponking66.proto3.NatProxyProtos;
import org.github.ponking66.proto3.ProtocType;

/**
 * @author pony
 * @date 2023/4/28
 */
public class ClientTunnelBindHandler extends ProtoHandler {

    /**
     * TCP协议相关的目标服务器相关的启动器
     */
    protected final TargetServerListener tcpTargetSeverListener;

    /**
     * UDP协议相关的目标服务器相关的启动器
     */
    protected final TargetServerListener udpTargetSeverListener;

    public ClientTunnelBindHandler(Bootstrap proxyServerBootstrap) {
        tcpTargetSeverListener = new TcpTargetSeverListener(proxyServerBootstrap);
        udpTargetSeverListener = new UdpTargetSeverListener(proxyServerBootstrap);
    }

    @Override
    public void handleRead(ChannelHandlerContext ctx, NatProxyProtos.Packet packet) {
        NatProxyProtos.ProxyTunnelInfoRequest request = packet.getProxyTunnelInfoRequest();
        Channel cmdChannel = ctx.channel();
        String type = request.getType();
        ProxyTunnelInfoReq message = new ProxyTunnelInfoReq();

        message.setHost(request.getHost());
        message.setPort(request.getPort());
        message.setToken(request.getToken());
        message.setType(request.getType());
        message.setUserId(request.getUserId());

        if (ProtocType.TCP.equals(type)) {
            tcpTargetSeverListener.listen(cmdChannel, message);
        } else if (ProtocType.UDP.equals(type)) {
            udpTargetSeverListener.listen(cmdChannel, message);
        } else {
            LOGGER.warn("Illegal agreement. ProtocType value is {}.", type);
        }

    }

    @Override
    public NatProxyProtos.Header.MessageType getMessageType() {
        return NatProxyProtos.Header.MessageType.CONNECT_REQUEST;
    }
}
