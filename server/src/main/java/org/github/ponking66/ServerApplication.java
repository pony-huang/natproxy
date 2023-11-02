package org.github.ponking66;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
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
import org.github.ponking66.proto3.NatProxyProtos;
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
        initBootstrapServer();
        TLSConfig tls = ProxyConfig.server().getTls();
        if (tls != null && tls.isEnable() &&
                ObjectUtils.isNotEmpty(tls.getCaFile()) &&
                ObjectUtils.isNotEmpty(tls.getKeyFile()) &&
                ObjectUtils.isNotEmpty(tls.getKeyCertChainFile())) {
            initSSLBootstrapServer(tls);
        }
    }

    private void initBootstrapServer() throws InterruptedException, ExecutionException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new ProtobufVarint32FrameDecoder())
                                .addLast(new ProtobufDecoder(NatProxyProtos.Packet.getDefaultInstance()))
                                .addLast(new ProtobufVarint32LengthFieldPrepender())
                                .addLast(new ProtobufEncoder());
                        pipeline.addLast(new IdleStateHandler(ProxyConfig.READER_IDLE_TIME_SECONDS, ProxyConfig.WRITER_IDLE_TIME_SECONDS, ProxyConfig.ALL_IDLE_TIME_SECONDS));
                        pipeline.addLast(new ServerLoginAuthHandler(userApplications));
                        pipeline.addLast(new ServerDisconnectHandler());
                        pipeline.addLast(new ServerTunnelConnectHandler());
                        pipeline.addLast(new ServerTunnelTransferHandler());
                        pipeline.addLast(new HeartBeatServerHandler());
                    }
                });
        bootstrap.bind(ProxyConfig.getServerPort()).get();
    }

    public void initSSLBootstrapServer(TLSConfig tls) throws InterruptedException, IOException, ExecutionException {
        File crt = new File(tls.getKeyCertChainFile());
        File key = new File(tls.getKeyFile());
        File ca = new File(tls.getCaFile());
        SslContext sslContext = SslContextBuilder.forServer(crt, key).trustManager(ca).clientAuth(ClientAuth.REQUIRE).build();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new SslHandler(sslContext.newEngine(ch.alloc())));
                        pipeline.addLast(new ProtobufVarint32FrameDecoder())
                                .addLast(new ProtobufDecoder(NatProxyProtos.Packet.getDefaultInstance()))
                                .addLast(new ProtobufVarint32LengthFieldPrepender())
                                .addLast(new ProtobufEncoder());

                        pipeline.addLast(new IdleStateHandler(ProxyConfig.READER_IDLE_TIME_SECONDS, ProxyConfig.WRITER_IDLE_TIME_SECONDS, ProxyConfig.ALL_IDLE_TIME_SECONDS));
                        pipeline.addLast(new ServerLoginAuthHandler(userApplications));
                        pipeline.addLast(new ServerDisconnectHandler());
                        pipeline.addLast(new ServerTunnelConnectHandler());
                        pipeline.addLast(new ServerTunnelTransferHandler());
                        pipeline.addLast(new HeartBeatServerHandler());
                    }
                });

        bootstrap.bind(tls.getPort()).get();
    }

    @Override
    public void stop() throws Exception {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
