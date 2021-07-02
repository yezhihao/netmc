package io.github.yezhihao.netmc.codec;

import io.github.yezhihao.netmc.session.Session;
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
 * home https://gitee.com/yezhihao/jt808-server
 */
@ChannelHandler.Sharable
public class MessageEncoderWrapper extends ChannelOutboundHandlerAdapter {

    private MessageEncoder encoder;

    public MessageEncoderWrapper(MessageEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        ByteBuf buf = null;
        try {
            if (msg instanceof ByteBuf)
                buf = (ByteBuf) msg;
            else
                buf = encoder.encode(msg, ctx.channel().attr(Session.KEY).get());

            if (buf.isReadable()) {
                ctx.write(buf, promise);
            } else {
                buf.release();
                ctx.write(Unpooled.EMPTY_BUFFER, promise);
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