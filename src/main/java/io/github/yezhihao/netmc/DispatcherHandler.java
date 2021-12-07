package io.github.yezhihao.netmc;

import io.github.yezhihao.netmc.core.HandlerInterceptor;
import io.github.yezhihao.netmc.core.HandlerMapping;
import io.github.yezhihao.netmc.core.handler.Handler;
import io.github.yezhihao.netmc.core.model.Message;
import io.github.yezhihao.netmc.session.Packet;
import io.github.yezhihao.netmc.session.Session;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
@ChannelHandler.Sharable
public class DispatcherHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(DispatcherHandler.class.getSimpleName());

    private final HandlerMapping handlerMapping;

    private final HandlerInterceptor interceptor;

//    private final AtomicInteger count = new AtomicInteger();

    public static DispatcherHandler newInstance(HandlerMapping handlerMapping, HandlerInterceptor interceptor, ExecutorService executor) {
        if (executor == null)
            return new DispatcherHandler(handlerMapping, interceptor);
        return new AsyncImpl(handlerMapping, interceptor, executor);
    }

    private DispatcherHandler(HandlerMapping handlerMapping, HandlerInterceptor interceptor) {
        this.handlerMapping = handlerMapping;
        this.interceptor = interceptor;
//        long start = System.currentTimeMillis();
//        Thread t = new Thread(() -> {
//            while (true) {
//                try {
//                    Thread.sleep(2000L);
//                } catch (Exception e) {
//                }
//                int num = count.get();
//                long time = (System.currentTimeMillis() - start) / 1000;
//                log.error(time + "\t" + num + "\t" + num / time);
//            }
//        });
//        t.setName(Thread.currentThread().getName() + "-c");
//        t.setPriority(Thread.MIN_PRIORITY);
//        t.setDaemon(true);
//        t.start();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        channelRead0(ctx, (Packet) msg);
    }

    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
//        count.addAndGet(1);
        Session session = packet.session;
        Message request = packet.message;
        Message response;
        long time = session.access();

        try {
            Handler handler = handlerMapping.getHandler(request.getMessageId());
            if (handler != null) {
                if (!interceptor.beforeHandle(request, session))
                    return;

                response = handler.invoke(request, session);
                if (handler.returnVoid) {
                    response = interceptor.successful(request, session);
                } else {
                    interceptor.afterHandle(request, response, session);
                }
            } else {
                response = interceptor.notSupported(request, session);
            }
        } catch (Exception e) {
            log.warn(String.valueOf(request), e);
            response = interceptor.exceptional(request, session, e);
        }
        time = System.currentTimeMillis() - time;
        if (time > 200)
            log.info("====={},处理耗时{}ms,", request.getMessageName(), time);
        if (response != null)
            ctx.writeAndFlush(packet.replace(response));
    }

    private static class AsyncImpl extends DispatcherHandler {

        private final ExecutorService executor;

        private AsyncImpl(HandlerMapping handlerMapping, HandlerInterceptor interceptor, ExecutorService executor) {
            super(handlerMapping, interceptor);
            this.executor = executor;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            executor.execute(() -> channelRead0(ctx, (Packet) msg));
        }
    }
}