package io.github.yezhihao.netmc.codec;

import io.github.yezhihao.netmc.core.model.Message;
import io.github.yezhihao.netmc.session.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;

/**
 * 基础消息解码
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
@ChannelHandler.Sharable
public class MessageDecoderWrapper extends ChannelInboundHandlerAdapter {

    private final MessageDecoder decoder;

    public MessageDecoderWrapper(MessageDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Packet packet = (Packet) msg;
        ByteBuf buf = packet.take();
        try {
            Message message = decoder.decode(buf, packet.session);
            if (message != null)
                ctx.fireChannelRead(packet.replace(message));
            buf.skipBytes(buf.readableBytes());
        } catch (Exception e) {
            throw new DecoderException(ByteBufUtil.hexDump(buf), e);
        } finally {
            buf.release();
        }
    }
}