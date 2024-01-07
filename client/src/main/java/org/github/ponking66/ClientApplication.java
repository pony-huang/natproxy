package org.github.ponking66;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.github.ponking66.common.ProxyConfig;
import org.github.ponking66.common.TLSConfig;
import org.github.ponking66.handler.*;
import org.github.ponking66.protoctl.NettyMessage;
import org.github.ponking66.protoctl.NettyMessageDecoder;
import org.github.ponking66.protoctl.NettyMessageEncoder;
import org.github.ponking66.util.ObjectUtils;
import org.github.ponking66.util.RequestResponseUtils;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.security.Security;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author pony
 * @date 2023/5/9
 */
public class ClientApplication implements Application {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 代理客户端启动器
     */
    private final Bootstrap proxyServerBootstrap = new Bootstrap();

    private final EventLoopGroup workerGroup;

    private final String host;

    private final int port;

    private final ExecutorService executor = Executors.newScheduledThreadPool(1);

    private volatile boolean isConnectedSuccessfully = false;

    private final AtomicInteger errorTimes = new AtomicInteger(0);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws InterruptedException {
        new ClientApplication(ProxyConfig.getServerHost(), isTlsEnable(ProxyConfig.client().getTls()) ? ProxyConfig.client().getTls().getPort() : ProxyConfig.getServerPort()).start();
    }

    public ClientApplication(String host, int port) {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("Host cannot be null or empty");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port is out of range");
        }
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
                        TLSConfig tls = ProxyConfig.client().getTls();
                        if (isTlsEnable(tls)) {
                            File crt = new File(tls.getKeyCertChainFile());
                            File key = new File(tls.getKeyFile());
                            File ca = new File(tls.getCaFile());
                            if (!crt.exists() || !crt.canRead() || !key.exists() || !key.canRead() || !ca.exists() || !ca.canRead()) {
                                throw new IOException("TLS files are not valid or not readable");
                            }
                            SslContext sslContext;
                            try {
                                sslContext = SslContextBuilder.forClient().keyManager(crt, key).trustManager(ca).build();
                            } catch (SSLException e) {
                                throw new RuntimeException("Failed to create SslContext", e);
                            }
                            pipeline.addLast(new SslHandler(sslContext.newEngine(ch.alloc())));
                        }
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


    private static boolean isTlsEnable(TLSConfig tls) {
        return tls != null && tls.isEnable() &&
                ObjectUtils.isNotEmpty(tls.getCaFile()) &&
                ObjectUtils.isNotEmpty(tls.getKeyFile()) &&
                ObjectUtils.isNotEmpty(tls.getKeyCertChainFile());
    }

    public void connect() throws InterruptedException {
        ChannelFuture cf = proxyServerBootstrap.connect(host, port);
        executor.execute(() -> {
            while (!isConnectedSuccessfully) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                    if (!isConnectedSuccessfully) {
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
                isConnectedSuccessfully = true;
                errorTimes.set(0);
                // Login
                NettyMessage message = RequestResponseUtils.loginRep(ProxyConfig.client().getKey());
                channel.writeAndFlush(message);
            } else {
                long delay = delay();
                LOGGER.info("Connection failure, try again after {} milliseconds", delay);
                channel.eventLoop().schedule(() -> {
                    try {
                        connect();
                    } catch (Exception e) {
                        // Handle exception, e.g., print stack trace or log the error.
                        LOGGER.error("Failed to connect", e);
                    }
                }, delay, TimeUnit.MILLISECONDS);
                isConnectedSuccessfully = false;
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
            LOGGER.error("Failed to stop", e);
        }
    }
}
