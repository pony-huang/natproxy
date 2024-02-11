package org.github.ponking66.core;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.github.ponking66.handler.TargetTcpServerChannelHandler;
import org.github.ponking66.pojo.ProxyTunnelInfoReq;
import org.github.ponking66.protoctl.NettyMessage;


/**
 * @author pony
 * @date 2023/5/22
 */
public class TcpTargetSeverListener implements TargetServerListener {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 代理客户端目标服务器的启动器
     */
    protected final Bootstrap targetServerBootstrap = new Bootstrap();

    /**
     * 代理客户端代理服务器的启动器
     */
    protected final Bootstrap proxyServerBootstrap;


    public TcpTargetSeverListener(Bootstrap proxyServerBootstrap) {
        this.proxyServerBootstrap = proxyServerBootstrap;
        targetServerBootstrap.group(proxyServerBootstrap.config().group())
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new TargetTcpServerChannelHandler());
                    }
                });
    }

    @Override
    public void listen(Channel cmdChannel, NettyMessage message) {
        ProxyTunnelInfoReq proxyTunnelInfoReq = (ProxyTunnelInfoReq) message.getBody();
        final String userId = proxyTunnelInfoReq.getUserId();
        // 目标服务器的 ip 和 port
        String ip = proxyTunnelInfoReq.getHost();
        int port = proxyTunnelInfoReq.getPort();
        // 连接目标服务器
        targetServerBootstrap.connect(ip, port).addListener(new TargetServerChannelFutureListener(cmdChannel, userId, proxyServerBootstrap));
    }

}
