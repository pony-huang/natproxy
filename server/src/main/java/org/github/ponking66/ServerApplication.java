package org.github.ponking66;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.github.ponking66.common.ProxyConfig;
import org.github.ponking66.common.TLSConfig;
import org.github.ponking66.core.UserApplication;
import org.github.ponking66.core.UsersTcpBootstrapApplication;
import org.github.ponking66.core.UsersUdpBootstrapApplication;
import org.github.ponking66.handler.*;
import org.github.ponking66.protoctl.NettyMessageDecoder;
import org.github.ponking66.protoctl.NettyMessageEncoder;
import org.github.ponking66.util.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author pony
 * @date 2023/5/9
 */
public class ServerApplication implements Application {

    private static final Logger LOGGER = LogManager.getLogger();

    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private final List<UserApplication> userApplications;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws Exception {
        new ServerApplication().start();
    }

    public ServerApplication() {
        userApplications = Arrays.asList(new UsersTcpBootstrapApplication(bossGroup, workerGroup), new UsersUdpBootstrapApplication(workerGroup));
    }

    @Override
    public void start() throws Exception {
        initServerBootstrap(false, null, ProxyConfig.server().getPort());
        LOGGER.info("server start success");
        TLSConfig tls = ProxyConfig.server().getTls();
        if (tls != null && tls.isEnable() &&
                ObjectUtils.isNotEmpty(tls.getCaFile()) &&
                ObjectUtils.isNotEmpty(tls.getKeyFile()) &&
                ObjectUtils.isNotEmpty(tls.getKeyCertChainFile())) {
            initServerBootstrap(true, tls, ProxyConfig.server().getTls().getPort());
            LOGGER.info("ssl server start success");
        }
    }

    /**
     * 初始化服务器引导程序
     *
     * @param isSsl 是否使用SSL
     * @param tls TLS配置
     * @param port 端口号
     * @throws InterruptedException 线程中断异常
     * @throws ExecutionException  执行异常
     * @throws IOException          IO异常
     */
    private void initServerBootstrap(boolean isSsl, TLSConfig tls, int port) throws InterruptedException, ExecutionException, IOException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        if (isSsl) {
                            File crt = new File(tls.getKeyCertChainFile());
                            File key = new File(tls.getKeyFile());
                            File ca = new File(tls.getCaFile());
                            SslContext sslContext = SslContextBuilder.forServer(crt, key).trustManager(ca).clientAuth(ClientAuth.REQUIRE).build();
                            pipeline.addLast(new SslHandler(sslContext.newEngine(ch.alloc())));
                        }
                        pipeline.addLast(new NettyMessageDecoder());
                        pipeline.addLast(new NettyMessageEncoder());
                        pipeline.addLast(new IdleStateHandler(ProxyConfig.READER_IDLE_TIME_SECONDS, ProxyConfig.WRITER_IDLE_TIME_SECONDS, ProxyConfig.ALL_IDLE_TIME_SECONDS));
                        pipeline.addLast(new ServerLoginAuthHandler(userApplications));
                        pipeline.addLast(new ServerDisconnectHandler());
                        pipeline.addLast(new ServerTunnelConnectHandler());
                        pipeline.addLast(new ServerTunnelTransferHandler());
                        pipeline.addLast(new HeartBeatServerHandler());
                    }
                });
        bootstrap.bind(port).get();
    }

    @Override
    public void stop() throws Exception {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
