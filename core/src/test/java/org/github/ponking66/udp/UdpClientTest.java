package org.github.ponking66.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * @author pony
 * @date 2023/5/21
 */
public class UdpClientTest {


    @Test
    public void test() {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioDatagramChannel.class)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        protected void initChannel(NioDatagramChannel ch) throws Exception {
                            ch.pipeline().addLast(new MyClientHandler());
                        }
                    });

            ChannelFuture channelFuture = b.bind(0);


            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        System.out.println(future.channel().hashCode());
                    }
                }
            });
            Channel ch = channelFuture.sync().channel();
            System.out.println(ch.hashCode());


            int count = 0;
            //向目标端口发送信息
            while (true) {
                InetSocketAddress recipient = new InetSocketAddress("192.168.31.102", 55555);
                InetSocketAddress sender = new InetSocketAddress("192.168.111.111", 55555);
                ch.writeAndFlush(
                        new DatagramPacket(
                                Unpooled.copiedBuffer("Good morning Sir! " + ++count, StandardCharsets.UTF_8),
                                recipient, sender)).sync();
                TimeUnit.SECONDS.sleep(3);
                System.out.println("Times --- " + count);
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    @Test
    public void test2() {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioDatagramChannel.class)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        protected void initChannel(NioDatagramChannel ch) throws Exception {
                            ch.pipeline().addLast(new MyClientHandler());
                        }
                    });

            ChannelFuture channelFuture = b.connect("192.168.31.102", 44444);


            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        System.out.println(future.channel().hashCode());
                    }
                }
            });
            Channel ch = channelFuture.sync().channel();
            System.out.println(ch.hashCode());


            int count = 0;
            //向目标端口发送信息
            while (true) {
                ch.writeAndFlush(
                        new DatagramPacket(
                                Unpooled.copiedBuffer("Good morning Sir! " + ++count, StandardCharsets.UTF_8),
                                (InetSocketAddress) ch.remoteAddress())).sync();
                TimeUnit.SECONDS.sleep(3);
                System.out.println("Times --- " + count);
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    @Test
    public void test3(){
        InetSocketAddress i1 = new InetSocketAddress("127.0.0.1", 8888);
        InetSocketAddress i2 = new InetSocketAddress("127.0.0.1", 8888);
        InetSocketAddress i3 = new InetSocketAddress("127.0.0.1", 4444);
        InetSocketAddress i4 = new InetSocketAddress("192.168.31.102", 8888);
        System.out.println(i1.equals(i2));
        System.out.println(i1.equals(i3));
        System.out.println(i1.equals(i4));

    }
}
