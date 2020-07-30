/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.example.echo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;

/**
 * Handler implementation for the echo server.
 */
@Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式


    // 创建一个自定义的线程组
    private DefaultEventExecutorGroup executors = new DefaultEventExecutorGroup(16);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
        System.out.println(String.format("【%s】start---->%s", ctx.channel().remoteAddress(), df.format(new Date())));
//            ctx.write(msg);
//        System.out.println("当前线程池：" + Thread.currentThread().getName());
//        executors.submit(() -> {
        System.out.println("添加到新的线程池：" + Thread.currentThread().getName());
        ByteBuf byteBuf = (ByteBuf) msg;
        byte[] req = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(req);
        String body = new String(req, CharsetUtil.UTF_8);
        // 阻塞 10 秒
        Thread.sleep(10000);
        System.out.println(body + " " + Thread.currentThread().getName());
        String reqString = "Hello I am Server";
        logger.error("EchoServerHandler printing");
        ByteBuf resp = Unpooled.copiedBuffer(reqString.getBytes());
        ctx.writeAndFlush(resp);
        System.out.println(String.format("【%s】end---->%s", ctx.channel().remoteAddress(), df.format(new Date())));
//        return null;
//        });
//        Thread.sleep(5000);
//        System.out.println(String.format("【%s】channelRead end ---->%s", ctx.channel().remoteAddress(),df.format(new Date())) );

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        logger.error("EchoServerHandler readComplete");
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}
