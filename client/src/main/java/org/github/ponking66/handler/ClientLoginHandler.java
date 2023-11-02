package org.github.ponking66.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.github.ponking66.Application;
import org.github.ponking66.core.ClientChannelManager;


import org.github.ponking66.proto3.NatProxyProtos;
import org.github.ponking66.proto3.ProtoRequestResponseHelper;

import java.util.concurrent.TimeUnit;

/**
 * @author pony
 * @date 2023/5/18
 */
public class ClientLoginHandler extends ProtoHandler {

    private static final Logger LOGGER = LogManager.getLogger();
    private volatile ScheduledFuture<?> heartBeat;
    private final Application clientApplication;

    public ClientLoginHandler(Application clientApplication) {
        this.clientApplication = clientApplication;
    }

    @Override
    public void handleRead(ChannelHandlerContext ctx, NatProxyProtos.Packet packet) throws Exception {
        NatProxyProtos.Header.Status status = packet.getHeader().getStatus();
        if (status != NatProxyProtos.Header.Status.SUCCESS) {
            LOGGER.info("Login failed, reason: {}", packet.getLoginResponse().getError());
            if (heartBeat != null) {
                heartBeat.cancel(true);
            }
            ctx.close();
            clientApplication.stop();
        } else {
            LOGGER.info("Login success!");
            // 保存控制服务channel
            ClientChannelManager.setCmdChannel(ctx.channel());
            heartBeat = ctx.executor().scheduleAtFixedRate(new HeartBeatTask(ctx), 0, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public NatProxyProtos.Header.MessageType getMessageType() {
        return NatProxyProtos.Header.MessageType.LOGIN_RESPONSE;
    }

    private record HeartBeatTask(ChannelHandlerContext ctx) implements Runnable {
        @Override
        public void run() {
            ctx.writeAndFlush(ProtoRequestResponseHelper.heartbeatRequest());
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

}
