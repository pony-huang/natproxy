package org.github.ponking66.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.github.ponking66.proto3.NatProxyProtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author pony
 * @date 2023/5/7
 */
public class HeartBeatServerHandler extends SimpleChannelInboundHandler<NatProxyProtos.Packet> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartBeatServerHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NatProxyProtos.Packet message) throws Exception {
        if (message.getHeader().getType() == NatProxyProtos.Header.MessageType.HEARTBEAT_REQUEST) {
            LOGGER.info("Receive client heart beat message.");
        } else {
            ctx.fireChannelRead(message);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                LOGGER.warn("Channel read timeout {}", ctx.channel());
                ctx.disconnect();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
