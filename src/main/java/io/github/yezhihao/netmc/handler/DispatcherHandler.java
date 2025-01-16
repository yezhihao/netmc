package io.github.yezhihao.netmc.handler;

import io.github.yezhihao.netmc.core.HandlerInterceptor;
import io.github.yezhihao.netmc.core.HandlerMapping;
import io.github.yezhihao.netmc.core.handler.Handler;
import io.github.yezhihao.netmc.core.model.Message;
import io.github.yezhihao.netmc.session.Packet;
import io.github.yezhihao.netmc.session.Session;
import io.github.yezhihao.netmc.util.Stopwatch;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
@ChannelHandler.Sharable
public class DispatcherHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(DispatcherHandler.class);

    private final HandlerMapping handlerMapping;

    private final HandlerInterceptor interceptor;

    private final ExecutorService executor;

    public static boolean STOPWATCH = false;

    private static Stopwatch s;

    public DispatcherHandler(HandlerMapping handlerMapping, HandlerInterceptor interceptor, ExecutorService executor) {
        if (STOPWATCH && s == null)
            s = new Stopwatch().start();
        this.handlerMapping = handlerMapping;
        this.interceptor = interceptor;
        this.executor = executor;
    }

    @Override
    public final void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (STOPWATCH)
            s.increment();

        Packet packet = (Packet) msg;
        Message request = packet.message;
        Handler handler = handlerMapping.getHandler(request.getMessageId());

        if (handler == null) {
            Message response = interceptor.notSupported(request, packet.session);
            if (response != null) {
                ctx.writeAndFlush(packet.replace(response));
            }
        } else {
            if (handler.async) {
                executor.execute(() -> channelRead0(ctx, packet, handler));
            } else {
                channelRead0(ctx, packet, handler);
            }
        }
    }

    private void channelRead0(ChannelHandlerContext ctx, Packet packet, Handler handler) {
        Session session = packet.session;
        Message request = packet.message;
        Message response;
        long time = System.currentTimeMillis();

        try {
            if (!interceptor.beforeHandle(request, session))
                return;

            response = handler.invoke(request, session);
            if (handler.returnVoid) {
                response = interceptor.successful(request, session);
            } else {
                interceptor.afterHandle(request, response, session);
            }
        } catch (InvocationTargetException e) {
            log.warn(String.valueOf(request), e.getTargetException());
            response = interceptor.exceptional(request, session, e.getTargetException());
        } catch (Exception e) {
            log.warn(String.valueOf(request), e);
            response = interceptor.exceptional(request, session, e);
        }
        time = System.currentTimeMillis() - time;
        if (time > 100)
            log.info("====={},慢处理耗时{}ms", handler, time);
        if (response != null)
            ctx.writeAndFlush(packet.replace(response));
    }
}