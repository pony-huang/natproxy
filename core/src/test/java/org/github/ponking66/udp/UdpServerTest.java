package org.github.ponking66.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.junit.Test;

/**
 * @author pony
 * @date 2023/5/21
 */
public class UdpServerTest {


    @Test
    public void test() {
        Bootstrap bootstrap = new Bootstrap();

        NioEventLoopGroup workGroup = new NioEventLoopGroup();
        bootstrap.group(workGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)    //广播
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel ch) throws Exception {
                        ch.pipeline().addLast(new MyServerHandler());
                    }
                });

        try {
            bootstrap.bind(44444).sync().channel().closeFuture().await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }
}
