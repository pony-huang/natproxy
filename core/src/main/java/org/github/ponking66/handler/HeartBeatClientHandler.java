package org.github.ponking66.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.ScheduledFuture;
import org.github.ponking66.protoctl.Header;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author pony
 * @date 2023/5/7
 */
public class HeartBeatClientHandler extends SimpleChannelInboundHandler<NettyMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartBeatClientHandler.class);

    private volatile ScheduledFuture<?> heartBeat;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NettyMessage message) throws Exception {
        // 握手成功，主动发送心跳消息
        if (message.getHeader().getType() == MessageType.LOGIN_RESPONSE) {
            heartBeat = ctx.executor().scheduleAtFixedRate(new HeartBeatTask(ctx), 0, 5000, TimeUnit.MILLISECONDS);
        } else if (message.getHeader().getType() == MessageType.HEARTBEAT_RESPONSE) {
            LOGGER.info("Client receive server heart beat message.");
        } else {
            ctx.fireChannelRead(message);
        }
    }

    private record HeartBeatTask(ChannelHandlerContext ctx) implements Runnable {
        @Override
        public void run() {
            ctx.writeAndFlush(buildHeatBeat());
            LOGGER.info("Client send heart beat message to server.");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        if (heartBeat != null) {
            heartBeat.cancel(true);
            heartBeat = null;
        }
        ctx.fireExceptionCaught(cause);
    }

    private static NettyMessage buildHeatBeat() {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(MessageType.HEARTBEAT_REQUEST);
        message.setHeader(header);
        return message;
    }
}
