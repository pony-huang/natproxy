package org.github.ponking66;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.github.ponking66.common.ProxyConfig;
import org.github.ponking66.core.UsersApplication;
import org.github.ponking66.core.UsersTcpBootstrapApplication;
import org.github.ponking66.core.UsersUdpBootstrapApplication;
import org.github.ponking66.handler.*;
import org.github.ponking66.protoctl.NettyMessageDecoder;
import org.github.ponking66.protoctl.NettyMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * @author pony
 * @date 2023/5/9
 */
public class ServerApplication {

    protected final Logger LOGGER = LoggerFactory.getLogger(ServerApplication.class);

    static {
        System.setProperty(ProxyConfig.ENV_PROPERTIES_CONFIG_FILE_NAME, ProxyConfig.SERVER_CONFIG_FILENAME);
        System.setProperty(ProxyConfig.ENV_PROPERTIES_LOG_FILE_NAME, ProxyConfig.SERVER_FILE_LOG);
    }

    public static void main(String[] args) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        UsersApplication tcpBootstrapApplication = new UsersTcpBootstrapApplication(bossGroup, workerGroup);
        UsersApplication udpBootstrapApplication = new UsersUdpBootstrapApplication(workerGroup);
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(8 * 1024, 32 * 1024))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new NettyMessageDecoder());
                            pipeline.addLast(new NettyMessageEncoder());
                            pipeline.addLast(new IdleStateHandler(ProxyConfig.READER_IDLE_TIME_SECONDS, ProxyConfig.WRITER_IDLE_TIME_SECONDS, ProxyConfig.ALL_IDLE_TIME_SECONDS));
                            pipeline.addLast(new ServerLoginAuthHandler(Arrays.asList(tcpBootstrapApplication, udpBootstrapApplication)));
                            pipeline.addLast(new ServerDisconnectHandler());
                            pipeline.addLast(new ServerTunnelConnectHandler());
                            pipeline.addLast(new ServerTunnelTransferHandler());
//                            pipeline.addLast(new ServerStatisticsChannelHandler());
                            pipeline.addLast(new HeartBeatServerHandler());
                        }
                    });

            Channel channel = bootstrap.bind(ProxyConfig.getServerPort()).sync().channel();
            channel.closeFuture().sync();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }

}
