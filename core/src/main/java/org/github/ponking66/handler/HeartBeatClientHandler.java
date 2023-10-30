package org.github.ponking66.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;

/**
 * @author pony
 * @date 2023/5/7
 */
public class HeartBeatClientHandler extends SimpleChannelInboundHandler<NettyMessage> {

        private static final Logger LOGGER = LogManager.getLogger();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NettyMessage message) throws Exception {
        // 握手成功，主动发送心跳消息
        if (message.getHeader().getType() == MessageType.HEARTBEAT_RESPONSE) {
            LOGGER.info("Client receive server heart beat message.");
        } else {
            ctx.fireChannelRead(message);
        }
    }

}
