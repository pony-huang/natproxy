package org.github.ponking66.handler;

import io.netty.channel.ChannelHandlerContext;
import org.github.ponking66.core.ClientChannelManager;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;

/**
 * @author pony
 * @date 2023/5/18
 */
public class ClientLoginAuthHandler extends BaseHandler {


    @Override
    public void handleRead(ChannelHandlerContext ctx, NettyMessage message) {
        if (message.getHeader().getStatus() != 200) {
            LOGGER.info("Login failed!");
            ctx.close();
        } else {
            ClientChannelManager.setCmdChannel(ctx.channel());
            LOGGER.info("Login success!");
        }
    }

    @Override
    public byte getMessageType() {
        return MessageType.LOGIN_RESPONSE;
    }
}
