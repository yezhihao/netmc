package io.github.yezhihao.netmc.codec;

import io.github.yezhihao.netmc.session.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.EncoderException;

/**
 * 基础消息编码
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
@ChannelHandler.Sharable
public class MessageEncoderWrapper extends ChannelOutboundHandlerAdapter {

    private final MessageEncoder encoder;

    public MessageEncoderWrapper(MessageEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        Packet packet = (Packet) msg;
        ByteBuf buf = packet.take();
        try {
            if (buf == null)
                buf = encoder.encode(packet.message, packet.session);

            if (buf.isReadable()) {
                ctx.write(packet.wrap(buf), promise);
            } else {
                buf.release();
                ctx.write(packet.wrap(Unpooled.EMPTY_BUFFER), promise);
            }
            buf = null;
        } catch (EncoderException e) {
            throw e;
        } catch (Throwable e) {
            throw new EncoderException(e);
        } finally {
            if (buf != null) {
                buf.release();
            }
        }
    }
}