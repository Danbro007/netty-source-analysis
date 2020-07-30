package io.netty.example.echo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Classname EchoOutServerHandler
 * @Description TODO
 * @Date 2020/7/26 10:51
 * @Author Danrbo
 */
public class EchoOutServerHandler extends ChannelOutboundHandlerAdapter {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        logger.error("out read");
        super.read(ctx);
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        logger.error("out flush");
        super.flush(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        logger.error("out write");
        super.write(ctx, msg, promise);
    }
}
