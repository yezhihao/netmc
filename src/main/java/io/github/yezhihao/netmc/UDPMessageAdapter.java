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

import java.net.InetAddress;
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

    private final int delay;

    public UDPMessageAdapter(SessionManager sessionManager, int readerIdleTime) {
        this.sessionManager = sessionManager;
        this.readerIdleTime = TimeUnit.SECONDS.toMillis(readerIdleTime);
        this.delay = Math.max(4, (readerIdleTime / 10));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        DatagramPacket packet = (DatagramPacket) msg;
        ByteBuf buf = packet.content();
        Session session = getSession(ctx, packet.sender());
        ctx.fireChannelRead(Packet.of(session, buf));
    }

    private final Map<InetAddress, Session> sessionMap = new ConcurrentHashMap<>();

    protected Session getSession(ChannelHandlerContext ctx, InetSocketAddress sender) {
        InetAddress address = sender.getAddress();
        Session session = sessionMap.get(address);
        if (session == null) {
            session = sessionManager.newInstance(ctx.channel(), sender, s -> sessionMap.remove(address, s));
            sessionMap.put(address, session);
            log.info("<<<<<终端连接{}", session);
        }
        return session;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.executor().scheduleWithFixedDelay(() -> {
            long now = System.currentTimeMillis();

            for (Map.Entry<InetAddress, Session> entry : sessionMap.entrySet()) {
                Session session = entry.getValue();

                long time = now - session.getLastAccessedTime();
                if (time >= readerIdleTime) {

                    log.warn(">>>>>终端心跳超时 {}", session);
                    session.invalidate();
                }
            }
        }, 0, delay, TimeUnit.SECONDS);
    }
}