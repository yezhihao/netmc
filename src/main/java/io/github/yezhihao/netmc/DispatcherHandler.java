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

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
@ChannelHandler.Sharable
public class DispatcherHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(DispatcherHandler.class.getSimpleName());

    private final HandlerMapping handlerMapping;

    private final HandlerInterceptor interceptor;

    public DispatcherHandler(HandlerMapping handlerMapping, HandlerInterceptor interceptor) {
        this.handlerMapping = handlerMapping;
        this.interceptor = interceptor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        channelRead0(ctx, (Packet) msg);
    }

    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
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
}