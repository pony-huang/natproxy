package org.github.ponking66;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import org.github.ponking66.common.ProxyConfig;
import org.github.ponking66.handler.*;
import org.github.ponking66.pojo.LoginRep;
import org.github.ponking66.protoctl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author pony
 * @date 2023/5/9
 */
public class ClientApplication implements Application {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    /**
     * 代理服务器启动器
     */
    private final Bootstrap proxyServerBootstrap = new Bootstrap();

    private final EventLoopGroup workerGroup;

    private final String host;

    private final int port;

    private final ExecutorService executor = Executors.newScheduledThreadPool(1);

    private volatile boolean success = false;

    private final AtomicInteger errorTimes = new AtomicInteger(0);

    static {
        System.setProperty(ProxyConfig.ENV_PROPERTIES_CONFIG_FILE_NAME, ProxyConfig.CLIENT_CONFIG_FILENAME);
        System.setProperty(ProxyConfig.ENV_PROPERTIES_LOG_FILE_NAME, ProxyConfig.CLIENT_FILE_LOG);
    }

    public static void main(String[] args) throws InterruptedException {
        new ClientApplication(ProxyConfig.getServerHost(), ProxyConfig.getServerPort()).start();
    }

    public ClientApplication(String host, int port) {
        this.port = port;
        this.host = host;
        workerGroup = new NioEventLoopGroup();
        proxyServerBootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new NettyMessageDecoder());
                        pipeline.addLast(new NettyMessageEncoder());
                        pipeline.addLast(new IdleStateHandler(ProxyConfig.READER_IDLE_TIME_SECONDS, ProxyConfig.WRITER_IDLE_TIME_SECONDS, ProxyConfig.ALL_IDLE_TIME_SECONDS));
                        pipeline.addLast(new ClientCenterHandler(ClientApplication.this));
                        pipeline.addLast(new ClientLoginAuthHandler(ClientApplication.this));
                        pipeline.addLast(new ClientTunnelBindHandler(proxyServerBootstrap));
                        pipeline.addLast(new ClientTunnelTransferHandler());
                        pipeline.addLast(new ClientDisconnectHandler());
                        pipeline.addLast(new HeartBeatClientHandler());
                    }
                });
    }

    public void connect() throws InterruptedException {
        ChannelFuture cf = proxyServerBootstrap.connect(host, port);
        executor.execute(() -> {
            while (!success) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                    if (!success) {
                        LOGGER.info("Connection wait");
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        cf.addListener((ChannelFutureListener) future -> {
            Channel channel = future.channel();
            if (future.isSuccess()) {
                LOGGER.info("Successful connection");
                // Reset
                success = true;
                errorTimes.set(0);
                // Login
                NettyMessage message = new NettyMessage();
                message.setHeader(new Header().setType(MessageType.LOGIN_REQUEST));
                message.setBody(new LoginRep(ProxyConfig.client().getKey()));
                channel.writeAndFlush(message);

            } else {
                LOGGER.info("Connection failure");
                channel.eventLoop().schedule(() -> {
                    LOGGER.info("Try retry connection");
                    try {
                        connect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, delay(), TimeUnit.MILLISECONDS);
                success = false;
                errorTimes.incrementAndGet();
            }
        });
        cf.channel().closeFuture().sync();
    }

    private long delay() {
        // 多次连接失败，重试连接默认延迟3秒执行
        long delay = 3;
        // 判断是否处于失败连接超过10次,超过十次每10秒进行重试
        int times = errorTimes.getAndIncrement();
        if (times <= 10) {
            return delay * 1000L;
        } else {
            return 1000L * 10;
        }
    }

    @Override
    public void start() throws InterruptedException {
        connect();
    }

    @Override
    public void stop() throws Exception {
        try {
            workerGroup.shutdownGracefully();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
