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
     * 平衡读写速度，防止内存占用过多，出现OOM
     */
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel targetServerChannel = ctx.channel();
        Channel proxyServerChannel = targetServerChannel.attr(AttrConstants.BIND_CHANNEL).get();
        if (proxyServerChannel != null) {
            boolean writable = targetServerChannel.isWritable();
            LOGGER.debug("TargetServerChannel is Writable: {}", writable);
            proxyServerChannel.config().setOption(ChannelOption.AUTO_READ, writable);
        }
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("exceptionCaught", cause);
        super.exceptionCaught(ctx, cause);
    }
}
