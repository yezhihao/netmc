package io.github.yezhihao.netmc.session;

import io.github.yezhihao.netmc.core.model.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public abstract class Packet {

    public final Session session;
    public Message message;
    public ByteBuf byteBuf;

    public static Packet of(Session session, Message message) {
        if (session.isUdp())
            return new UDP(session, message, null);
        return new TCP(session, message, null);
    }

    public static Packet of(Session session, ByteBuf message) {
        if (session.isUdp())
            return new UDP(session, null, message);
        return new TCP(session, null, message);
    }

    private Packet(Session session, Message message, ByteBuf byteBuf) {
        this.session = session;
        this.message = message;
        this.byteBuf = byteBuf;
    }

    public Packet replace(Message message) {
        this.message = message;
        return this;
    }

    public ByteBuf take() {
        ByteBuf temp = this.byteBuf;
        this.byteBuf = null;
        return temp;
    }

    public abstract Object wrap(ByteBuf byteBuf);

    private static class TCP extends Packet {
        private TCP(Session session, Message message, ByteBuf byteBuf) {
            super(session, message, byteBuf);
        }

        @Override
        public Object wrap(ByteBuf byteBuf) {
            return byteBuf;
        }
    }

    private static class UDP extends Packet {
        private UDP(Session session, Message message, ByteBuf byteBuf) {
            super(session, message, byteBuf);
        }

        @Override
        public Object wrap(ByteBuf byteBuf) {
            return new DatagramPacket(byteBuf, session.remoteAddress());
        }
    }
}