package io.github.yezhihao.netmc.handler;

import io.github.yezhihao.netmc.session.Packet;
import io.github.yezhihao.netmc.session.Session;
import io.github.yezhihao.netmc.session.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * TCP消息适配器
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
@ChannelHandler.Sharable
public class TCPMessageAdapter extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(TCPMessageAdapter.class.getSimpleName());

    private static final AttributeKey<Session> KEY = AttributeKey.newInstance(Session.class.getName());

    private final SessionManager sessionManager;

    public TCPMessageAdapter(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        Session session = getSession(ctx);
        ctx.fireChannelRead(Packet.of(session, buf));
    }

    private Session getSession(ChannelHandlerContext ctx) {
        Session session = ctx.channel().attr(KEY).get();
        if (session == null) {
            Channel channel = ctx.channel();
            session = sessionManager.newInstance(channel);
            channel.attr(KEY).set(session);
        }
        return session;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("<<<<<终端连接{}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Session session = ctx.channel().attr(KEY).get();
        if (session != null)
            session.invalidate();
        log.info(">>>>>断开连接{}", client(ctx));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        if (e instanceof IOException)
            log.warn("<<<<<终端断开连接{} {}", client(ctx), e.getMessage());
        else
            log.warn(">>>>>消息处理异常" + client(ctx), e);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            IdleState state = event.state();
            if (state == IdleState.READER_IDLE || state == IdleState.WRITER_IDLE || state == IdleState.ALL_IDLE) {
                log.warn(">>>>>终端心跳超时{} {}", state, client(ctx));
                ctx.close();
            }
        }
    }

    private static Object client(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        Session session = channel.attr(KEY).get();
        if (session != null)
            return session;
        return channel.remoteAddress();
    }
}