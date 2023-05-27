package org.github.ponking66.udp;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author pony
 * @date 2023/5/21
 */
public class MyServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("active");
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("inactive");
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
        String msg = packet.content().toString(StandardCharsets.UTF_8);
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " UDP服务端接收到消息：" + msg);
        //向客户端发送消息
        String json = "通知：我已经收到你的消息 ---- " + msg + "\r\n";
        // 由于数据报的数据是以字符数组传的形式存储的，所以传转数据
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket data = new DatagramPacket(Unpooled.copiedBuffer(bytes), packet.sender());
        ctx.writeAndFlush(data);//向客户端发送消息
    }

}
