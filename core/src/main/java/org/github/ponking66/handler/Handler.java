package org.github.ponking66.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.github.ponking66.protoctl.NettyMessage;

/**
 * @author pony
 * @date 2023/4/28
 */
public abstract class Handler extends SimpleChannelInboundHandler<NettyMessage> {

    protected static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NettyMessage message) throws Exception {
        byte type = message.getHeader().getType();
        if (type == getMessageType()) {
            handleRead(ctx, message);
        } else {
            ctx.fireChannelRead(message);
        }
    }


    public abstract void handleRead(ChannelHandlerContext ctx, NettyMessage message) throws Exception;

    public abstract byte getMessageType();

}
