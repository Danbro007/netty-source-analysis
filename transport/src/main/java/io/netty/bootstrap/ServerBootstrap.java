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
package io.netty.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * {@link Bootstrap} sub-class which allows easy bootstrap of {@link ServerChannel}
 *
 */
public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ServerBootstrap.class);

    private final Map<ChannelOption<?>, Object> childOptions = new LinkedHashMap<ChannelOption<?>, Object>();
    private final Map<AttributeKey<?>, Object> childAttrs = new LinkedHashMap<AttributeKey<?>, Object>();
    private final ServerBootstrapConfig config = new ServerBootstrapConfig(this);
    private volatile EventLoopGroup childGroup;
    private volatile ChannelHandler childHandler;

    public ServerBootstrap() { }

    private ServerBootstrap(ServerBootstrap bootstrap) {
        super(bootstrap);
        childGroup = bootstrap.childGroup;
        childHandler = bootstrap.childHandler;
        synchronized (bootstrap.childOptions) {
            childOptions.putAll(bootstrap.childOptions);
        }
        synchronized (bootstrap.childAttrs) {
            childAttrs.putAll(bootstrap.childAttrs);
        }
    }

    /**
     * Specify the {@link EventLoopGroup} which is used for the parent (acceptor) and the child (client).
     */
    @Override
    public ServerBootstrap group(EventLoopGroup group) {
        return group(group, group);
    }

    /**
     * Set the {@link EventLoopGroup} for the parent (acceptor) and the child (client). These
     * {@link EventLoopGroup}'s are used to handle all the events and IO for {@link ServerChannel} and
     * {@link Channel}'s.
     *
     * 给父（接收器） 和 子（客户端）设置 EventLoopGroup。这些 EventLoopGroup 被用于处理所有的事件
     * 以及 ServerChannel 和 Channel 的 IO。
     *
     */
    public ServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
        // BossWorkGroup 是由 AbstractBootstrap 的 group 配置
        super.group(parentGroup);
        if (childGroup == null) {
            throw new NullPointerException("childGroup");
        }
        if (this.childGroup != null) {
            throw new IllegalStateException("childGroup set already");
        }
        // ServerBootstrap 负责配置
        this.childGroup = childGroup;
        return this;
    }

    /**
     * Allow to specify a {@link ChannelOption} which is used for the {@link Channel} instances once they get created
     * (after the acceptor accepted the {@link Channel}). Use a value of {@code null} to remove a previous set
     * {@link ChannelOption}.
     *
     * 允许指定一个 ChannelOption，在通道实例被创建后使用(在接受者接受通道之后)。使用 value
     * 为 null 来删除之前设置的 ChannelOption 。
     *
     */
    public <T> ServerBootstrap childOption(ChannelOption<T> childOption, T value) {
        if (childOption == null) {
            throw new NullPointerException("childOption");
        }
        if (value == null) {
            synchronized (childOptions) {
                childOptions.remove(childOption);
            }
        } else {
            synchronized (childOptions) {
                childOptions.put(childOption, value);
            }
        }
        return this;
    }

    /**
     * Set the specific {@link AttributeKey} with the given value on every child {@link Channel}. If the value is
     * {@code null} the {@link AttributeKey} is removed
     */
    public <T> ServerBootstrap childAttr(AttributeKey<T> childKey, T value) {
        if (childKey == null) {
            throw new NullPointerException("childKey");
        }
        if (value == null) {
            childAttrs.remove(childKey);
        } else {
            childAttrs.put(childKey, value);
        }
        return this;
    }

    /**
     * Set the {@link ChannelHandler} which is used to serve the request for the {@link Channel}'s.
     *
     * 设置用于为通道的请求提供服务的通道处理器。
     *
     */
    public ServerBootstrap childHandler(ChannelHandler childHandler) {
        if (childHandler == null) {
            throw new NullPointerException("childHandler");
        }
        this.childHandler = childHandler;
        return this;
    }

    // 初始化通道
    @Override
    void init(Channel channel) throws Exception {
        // 对 ServerBootstrap 配置设置的参数数组
        final Map<ChannelOption<?>, Object> options = options0();
        // 把设置在 ServerBootstrap 上的参数设置到 NioServerSocketChannel 上并打印日志
        synchronized (options) {
            setChannelOptions(channel, options, logger);
        }
        // 把设置的属性放入 NioServerSocketChannel 上
        final Map<AttributeKey<?>, Object> attrs = attrs0();
        synchronized (attrs) {
            for (Entry<AttributeKey<?>, Object> e: attrs.entrySet()) {
                @SuppressWarnings("unchecked")
                AttributeKey<Object> key = (AttributeKey<Object>) e.getKey();
                channel.attr(key).set(e.getValue());
            }
        }
        // 获取 NioServerSocketChannel 的 Pipline
        ChannelPipeline p = channel.pipeline();
        // workGroup
        final EventLoopGroup currentChildGroup = childGroup;
        // 配置在 workGroup 的处理器
        final ChannelHandler currentChildHandler = childHandler;
        final Entry<ChannelOption<?>, Object>[] currentChildOptions;
        final Entry<AttributeKey<?>, Object>[] currentChildAttrs;
        // 把对 workGroup 设置的配置和属性转换成 Entry 数组
        synchronized (childOptions) {
            currentChildOptions = childOptions.entrySet().toArray(newOptionArray(childOptions.size()));
        }
        synchronized (childAttrs) {
            currentChildAttrs = childAttrs.entrySet().toArray(newAttrArray(childAttrs.size()));
        }
        // 添加一个通道初始化器，当通道初始化的时候会在当前通道 pipline 里添加一个从 ServerBootstrapConfig 获取的日志处理器
        //
        p.addLast(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(final Channel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                ChannelHandler handler = config.handler();
                if (handler != null) {
                    pipeline.addLast(handler);
                }
                // 让一个 EventLoop 执行添加 ServerBootstrapAcceptor 到 PipLine 的任务
                // ServerBootstrapAcceptor 的作用是当有新的客户端来连接时会给这个连接的通道设置好 TCP 参数、配置上处理器，
                // 然后把这个连接的通道注册进 workGroup 里，之后让 workGroup 里的 NioEventLoop 来真正处理这个请求。
                ch.eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.addLast(new ServerBootstrapAcceptor(
                                ch, currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs));
                    }
                });
            }
        });
    }

    @Override
    public ServerBootstrap validate() {
        super.validate();
        if (childHandler == null) {
            throw new IllegalStateException("childHandler not set");
        }
        if (childGroup == null) {
            logger.warn("childGroup is not set. Using parentGroup instead.");
            childGroup = config.group();
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private static Entry<AttributeKey<?>, Object>[] newAttrArray(int size) {
        return new Entry[size];
    }

    @SuppressWarnings("unchecked")
    private static Map.Entry<ChannelOption<?>, Object>[] newOptionArray(int size) {
        return new Map.Entry[size];
    }

    private static class ServerBootstrapAcceptor extends ChannelInboundHandlerAdapter {

        private final EventLoopGroup childGroup;
        private final ChannelHandler childHandler;
        private final Entry<ChannelOption<?>, Object>[] childOptions;
        private final Entry<AttributeKey<?>, Object>[] childAttrs;
        private final Runnable enableAutoReadTask;

        ServerBootstrapAcceptor(
                final Channel channel, EventLoopGroup childGroup, ChannelHandler childHandler,
                Entry<ChannelOption<?>, Object>[] childOptions, Entry<AttributeKey<?>, Object>[] childAttrs) {
            this.childGroup = childGroup;
            this.childHandler = childHandler;
            this.childOptions = childOptions;
            this.childAttrs = childAttrs;

            // Task which is scheduled to re-enable auto-read.
            // It's important to create this Runnable before we try to submit it as otherwise the URLClassLoader may
            // not be able to load the class because of the file limit it already reached.
            //
            // See https://github.com/netty/netty/issues/1328
            // 开启自动读取的任务
            enableAutoReadTask = new Runnable() {
                @Override
                public void run() {
                    // 给 NioServerSocketChannel 开启自动读取,自动调用 ChannelHandlerContext read()方法
                    channel.config().setAutoRead(true);
                }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            final Channel child = (Channel) msg;
            // 给客户端的通道添加处理器,就是对 ServerBootstrap.childHandler() 时设置的
            child.pipeline().addLast(childHandler);
            // 给客户端的通道设置 TCP 参数
            setChannelOptions(child, childOptions, logger);

            for (Entry<AttributeKey<?>, Object> e: childAttrs) {
                child.attr((AttributeKey<Object>) e.getKey()).set(e.getValue());
            }

            try {
                // 把客户端的通道注册进 workGroup 里的一个 EventLoop 并添加一个监听器来监听连接失败
                // 这里选择的 EventLoop 的策略是轮询
                childGroup.register(child).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            forceClose(child, future.cause());
                        }
                    }
                });
            } catch (Throwable t) {
                forceClose(child, t);
            }
        }

        private static void forceClose(Channel child, Throwable t) {
            child.unsafe().closeForcibly();
            logger.warn("Failed to register an accepted channel: {}", child, t);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final ChannelConfig config = ctx.channel().config();
            if (config.isAutoRead()) {
                // stop accept new connections for 1 second to allow the channel to recover
                // See https://github.com/netty/netty/issues/1328
                config.setAutoRead(false);
                ctx.channel().eventLoop().schedule(enableAutoReadTask, 1, TimeUnit.SECONDS);
            }
            // still let the exceptionCaught event flow through the pipeline to give the user
            // a chance to do something with it
            ctx.fireExceptionCaught(cause);
        }
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public ServerBootstrap clone() {
        return new ServerBootstrap(this);
    }

    /**
     * Return the configured {@link EventLoopGroup} which will be used for the child channels or {@code null}
     * if non is configured yet.
     *
     * 返回已设置的 EventLoopGroup，它将用于子通道。如果未配置，返回 null。
     *
     * @deprecated Use {@link #config()} instead.
     */
    @Deprecated
    public EventLoopGroup childGroup() {
        return childGroup;
    }

    final ChannelHandler childHandler() {
        return childHandler;
    }

    final Map<ChannelOption<?>, Object> childOptions() {
        return copiedMap(childOptions);
    }

    final Map<AttributeKey<?>, Object> childAttrs() {
        return copiedMap(childAttrs);
    }

    @Override
    public final ServerBootstrapConfig config() {
        return config;
    }
}
