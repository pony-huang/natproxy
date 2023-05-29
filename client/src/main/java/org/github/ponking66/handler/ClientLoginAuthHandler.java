package org.github.ponking66.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;
import org.github.ponking66.core.ClientChannelManager;
import org.github.ponking66.protoctl.Header;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author pony
 * @date 2023/5/18
 */
public class ClientLoginAuthHandler extends BaseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientLoginAuthHandler.class);
    private volatile ScheduledFuture<?> heartBeat;

    @Override
    public void handleRead(ChannelHandlerContext ctx, NettyMessage message) {
        if (message.getHeader().getStatus() != 200) {
            LOGGER.info("Login failed!");
            ctx.close();
        } else {
            ClientChannelManager.setCmdChannel(ctx.channel());
            LOGGER.info("Login success!");
            heartBeat = ctx.executor().scheduleAtFixedRate(new HeartBeatTask(ctx), 0, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public byte getMessageType() {
        return MessageType.LOGIN_RESPONSE;
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
