package io.github.yezhihao.netmc;

import io.github.yezhihao.netmc.session.Packet;
import io.github.yezhihao.netmc.session.Session;
import io.github.yezhihao.netmc.session.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * UDP消息适配器
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
@ChannelHandler.Sharable
public class UDPMessageAdapter extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(UDPMessageAdapter.class.getSimpleName());

    private final SessionManager sessionManager;

    private final long readerIdleTime;

    public UDPMessageAdapter(SessionManager sessionManager, int readerIdleTime) {
        this.sessionManager = sessionManager;
        this.readerIdleTime = TimeUnit.SECONDS.toMillis(readerIdleTime);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        DatagramPacket packet = (DatagramPacket) msg;
        ByteBuf buf = packet.content();
        Session session = getSession(ctx, packet.sender());
        ctx.fireChannelRead(Packet.of(session, buf));
    }

    private final Map<Object, Session> sessionMap = new ConcurrentHashMap<>();

    protected Session getSession(ChannelHandlerContext ctx, InetSocketAddress sender) {
        Session session = sessionMap.get(sender);
        if (session == null) {
            session = sessionManager.newInstance(ctx.channel(), sender, s -> sessionMap.remove(sender, s));
            sessionMap.put(sender, session);
            log.info("<<<<<终端连接{}", session);
        }
        return session;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Thread thread = new Thread(() -> {
            for (; ; ) {
                long nextDelay = readerIdleTime;
                long now = System.currentTimeMillis();

                for (Session session : sessionMap.values()) {
                    long time = readerIdleTime - (now - session.getLastAccessedTime());

                    if (time <= 0) {
                        log.warn(">>>>>终端心跳超时 {}", session);
                        session.invalidate();
                    } else {
                        nextDelay = Math.min(time, readerIdleTime);
                    }
                }
                try {
                    Thread.sleep(nextDelay);
                } catch (Throwable e) {
                    log.warn("IdleStateScheduler", e);
                }
            }
        });
        thread.setName(Thread.currentThread().getName() + "-c");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }
}