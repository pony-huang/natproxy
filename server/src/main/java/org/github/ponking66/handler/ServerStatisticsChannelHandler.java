package org.github.ponking66.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author pony
 * @date 2023/5/29
 */
public class ServerStatisticsChannelHandler extends ChannelInboundHandlerAdapter {

    private final static AtomicInteger count = new AtomicInteger(0);

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerStatisticsChannelHandler.class);

    private static Thread Statistics = null;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        int nums = count.getAndIncrement();
        LOGGER.debug("Add new Channel, the number of connections is {}", nums);
        if (LOGGER.isDebugEnabled() && Statistics == null) {
            Statistics = new Thread(() -> {
                while (true) {
                    try {
                        TimeUnit.SECONDS.sleep(10);
                        LOGGER.debug("Now the number of connections is {}", count.get());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            ctx.executor().execute(Statistics);
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        int nums = count.decrementAndGet();
        LOGGER.debug("The Channel is close, the number of connections is {}", nums);
        super.channelInactive(ctx);
    }
}
