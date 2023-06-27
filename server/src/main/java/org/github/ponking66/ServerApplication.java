package org.github.ponking66;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.github.ponking66.common.ProxyConfig;
import org.github.ponking66.common.TLSConfig;
import org.github.ponking66.core.UserApplication;
import org.github.ponking66.core.UsersTcpBootstrapApplication;
import org.github.ponking66.core.UsersUdpBootstrapApplication;
import org.github.ponking66.handler.*;
import org.github.ponking66.protoctl.NettyMessageDecoder;
import org.github.ponking66.protoctl.NettyMessageEncoder;
import org.github.ponking66.util.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author pony
 * @date 2023/5/9
 */
public class ServerApplication implements Application {

    protected final Logger LOGGER = LoggerFactory.getLogger(ServerApplication.class);

    static {
        System.setProperty(ProxyConfig.ENV_PROPERTIES_CONFIG_FILE_NAME, ProxyConfig.SERVER_CONFIG_FILENAME);
        System.setProperty(ProxyConfig.ENV_PROPERTIES_LOG_FILE_NAME, ProxyConfig.SERVER_FILE_LOG);
    }

    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private final List<UserApplication> userApplications;

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

    private void initBootstrapServer() throws IOException, InterruptedException, ExecutionException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        initCommonHandlers(pipeline);
                    }
                });
        bootstrap.bind(ProxyConfig.getServerPort()).get();
    }

    public void initSSLBootstrapServer(TLSConfig tls) throws InterruptedException, IOException, ExecutionException {

        File crt = new File(tls.getKeyCertChainFile());
        File key = new File(tls.getKeyFile());
        File ca = new File(tls.getCaFile());

        SslContext sslContext = SslContextBuilder.forClient().keyManager(crt, key).trustManager(ca).clientAuth(ClientAuth.REQUIRE).build();
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new SslHandler(sslContext.newEngine(ch.alloc())));
                        initCommonHandlers(pipeline);
                    }
                });

        bootstrap.bind(tls.getPort()).get();
    }

    private void initCommonHandlers(ChannelPipeline pipeline) throws IOException {
        pipeline.addLast(new NettyMessageDecoder());
        pipeline.addLast(new NettyMessageEncoder());
        pipeline.addLast(new IdleStateHandler(ProxyConfig.READER_IDLE_TIME_SECONDS, ProxyConfig.WRITER_IDLE_TIME_SECONDS, ProxyConfig.ALL_IDLE_TIME_SECONDS));
        pipeline.addLast(new ServerLoginAuthHandler(userApplications));
        pipeline.addLast(new ServerDisconnectHandler());
        pipeline.addLast(new ServerTunnelConnectHandler());
        pipeline.addLast(new ServerTunnelTransferHandler());
        pipeline.addLast(new HeartBeatServerHandler());
    }

    @Override
    public void stop() throws Exception {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
