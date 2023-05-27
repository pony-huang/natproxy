package org.github.ponking66.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.github.ponking66.protoctl.Header;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author pony
 * @date 2023/5/7
 */
public class HeartBeatServerHandler extends SimpleChannelInboundHandler<NettyMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartBeatServerHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NettyMessage message) throws Exception {
        if (message.getHeader().getType() == MessageType.HEARTBEAT_REQUEST) {
            LOGGER.info("Receive client heart beat message.");
            ctx.writeAndFlush(buildHeatBeat());
            LOGGER.info("Send heart beat response message to client.");
        } else {
            ctx.fireChannelRead(message);
        }
    }

    private NettyMessage buildHeatBeat() {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(MessageType.HEARTBEAT_RESPONSE);
        message.setHeader(header);
        return message;
    }
}
