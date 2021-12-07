package io.github.yezhihao.netmc;

import io.github.yezhihao.netmc.core.HandlerInterceptor;
import io.github.yezhihao.netmc.core.HandlerMapping;
import io.github.yezhihao.netmc.session.Packet;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ExecutorService;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public class AsyncDispatcherHandler extends DispatcherHandler {

    private final ExecutorService executor;

    public AsyncDispatcherHandler(HandlerMapping handlerMapping, HandlerInterceptor interceptor, ExecutorService executor) {
        super(handlerMapping, interceptor);
        this.executor = executor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        executor.execute(() -> channelRead0(ctx, (Packet) msg));
    }
}