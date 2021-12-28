package io.github.yezhihao.netmc.handler;

import io.github.yezhihao.netmc.codec.MessageEncoder;
import io.github.yezhihao.netmc.session.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.EncoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基础消息编码
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
@ChannelHandler.Sharable
public class MessageEncoderWrapper extends ChannelOutboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(MessageEncoderWrapper.class);

    private final MessageEncoder encoder;

    public MessageEncoderWrapper(MessageEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        Packet packet = (Packet) msg;
        ByteBuf output = packet.take();
        try {
            if (output == null)
                output = encoder.encode(packet.message, packet.session);

            if (output.isReadable()) {
                ctx.write(packet.wrap(output), promise);
            } else {
                output.release();
                ctx.write(packet.wrap(Unpooled.EMPTY_BUFFER), promise);
            }
            output = null;
        } catch (EncoderException e) {
            log.error("消息编码异常" + packet.message, e);
            throw e;
        } catch (Throwable e) {
            log.error("消息编码异常" + packet.message, e);
            throw new EncoderException(e);
        } finally {
            if (output != null) {
                output.release();
            }
        }
    }
}