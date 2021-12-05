package io.github.yezhihao.netmc.codec;

import io.github.yezhihao.netmc.UDPMessageAdapter;
import io.github.yezhihao.netmc.session.Packet;
import io.github.yezhihao.netmc.session.Session;
import io.github.yezhihao.netmc.session.SessionManager;
import io.github.yezhihao.netmc.util.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.DecoderException;

import java.util.LinkedList;
import java.util.List;

public class UDPDelimiterBasedFrameDecoder extends UDPMessageAdapter {

    private final Delimiter[] delimiters;

    public UDPDelimiterBasedFrameDecoder(SessionManager sessionManager, int readerIdleTime, Delimiter[] delimiters) {
        super(sessionManager, readerIdleTime);
        this.delimiters = delimiters;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        DatagramPacket packet = (DatagramPacket) msg;
        ByteBuf buf = packet.content();
        Session session = getSession(ctx, packet.sender());

        try {
            List<ByteBuf> out = decode(buf);
            for (ByteBuf t : out) {
                ctx.fireChannelRead(Packet.of(session, t));
            }
        } catch (DecoderException e) {
            throw e;
        } catch (Exception e) {
            throw new DecoderException(e);
        } finally {
            buf.release();
        }
    }

    protected List<ByteBuf> decode(ByteBuf in) {
        List<ByteBuf> out = new LinkedList<>();
        while (in.isReadable()) {

            for (Delimiter delim : delimiters) {
                int minDelimLength = delim.value.length;

                int frameLength = ByteBufUtils.indexOf(in, delim.value);
                if (frameLength >= 0) {

                    if (delim.strip) {
                        if (frameLength != 0)
                            out.add(in.readRetainedSlice(frameLength));
                        in.skipBytes(minDelimLength);
                    } else {
                        if (frameLength != 0) {
                            out.add(in.readRetainedSlice(frameLength + minDelimLength));
                        } else {
                            in.skipBytes(minDelimLength);
                        }
                    }
                } else {
                    int i = in.readableBytes();
                    if (i > 0)
                        out.add(in.readRetainedSlice(i));
                }
            }
        }
        return out;
    }
}