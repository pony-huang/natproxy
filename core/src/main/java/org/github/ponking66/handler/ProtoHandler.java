package org.github.ponking66.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.github.ponking66.proto3.NatProxyProtos;

/**
 * @author pony
 * @date 2023/4/28
 */
public abstract class ProtoHandler extends SimpleChannelInboundHandler<NatProxyProtos.Packet> {

    protected static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NatProxyProtos.Packet message) throws Exception {
        NatProxyProtos.Header.MessageType type = message.getHeader().getType();
        if (type == getMessageType()) {
            handleRead(ctx, message);
        } else {
            ctx.fireChannelRead(message);
        }
    }

    public abstract void handleRead(ChannelHandlerContext ctx, NatProxyProtos.Packet packet) throws Exception;

    public abstract NatProxyProtos.Header.MessageType getMessageType();

}
