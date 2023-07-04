package org.github.ponking66.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import org.github.ponking66.common.AttrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 处理代理客户端和目标服务器Message的Handler
 *
 * @author pony
 * @date 2023/5/29
 */
public abstract class AbstractTargetServerChannelHandler<T> extends SimpleChannelInboundHandler<T> {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    /**
     * 平衡读写速度
     */
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel targetServerChannel = ctx.channel();
        // 与代理服务器的Channel
        Channel proxyServerChannel = targetServerChannel.attr(AttrConstants.BIND_CHANNEL).get();
        if (proxyServerChannel != null) {
            boolean writable = targetServerChannel.isWritable();
            proxyServerChannel.config().setOption(ChannelOption.AUTO_READ, writable);
        }
        super.channelWritabilityChanged(ctx);
    }

}
