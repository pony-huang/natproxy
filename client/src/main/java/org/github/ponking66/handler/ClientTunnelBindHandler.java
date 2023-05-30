package org.github.ponking66.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.github.ponking66.core.TargetServerListener;
import org.github.ponking66.core.TcpTargetSeverListener;
import org.github.ponking66.core.UdpTargetSeverListener;
import org.github.ponking66.pojo.ProxyTunnelInfoReq;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;
import org.github.ponking66.protoctl.ProtocType;

/**
 * @author pony
 * @date 2023/4/28
 */
public class ClientTunnelBindHandler extends BaseHandler {

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
    public void handleRead(ChannelHandlerContext ctx, NettyMessage message) {
        ProxyTunnelInfoReq proxyTunnelInfoReq = (ProxyTunnelInfoReq) message.getBody();
        Channel cmdChannel = ctx.channel();
        String type = proxyTunnelInfoReq.getType();
        if (ProtocType.TCP.equals(type)) {
            tcpTargetSeverListener.listen(cmdChannel, message);
        } else if (ProtocType.UDP.equals(type)) {
            udpTargetSeverListener.listen(cmdChannel, message);
        } else {
            LOGGER.warn("Illegal agreement. ProtocType value is {}", type);
        }

    }

    @Override
    public byte getMessageType() {
        return MessageType.CONNECT_REQUEST;
    }
}
