package io.github.yezhihao.netmc.handler;

import io.github.yezhihao.netmc.util.IntTool;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.TooLongFrameException;

import java.util.List;
import java.util.function.ToIntFunction;

import static io.netty.util.internal.ObjectUtil.*;

/**
 * @see io.netty.handler.codec.LengthFieldBasedFrameDecoder
 */
public class DynamicLengthFieldBasedFrameDecoder extends ByteToMessageDecoder {

    private final int maxFrameLength;
    private final ToIntFunction<ByteBuf> lengthFieldOffsetGetter;
    private final int lengthFieldLength;
    private final IntTool lengthFieldLengthGetter;
    private final int lengthAdjustment;
    private final int initialBytesToStrip;
    private final boolean failFast;
    private boolean discardingTooLongFrame;
    private long tooLongFrameLength;
    private long bytesToDiscard;
    private int frameLengthInt = -1;

    public DynamicLengthFieldBasedFrameDecoder(int maxFrameLength, ToIntFunction<ByteBuf> lengthFieldOffsetGetter, int lengthFieldLength,
                                               int lengthAdjustment, int initialBytesToStrip, boolean failFast) {

        checkPositive(maxFrameLength, "maxFrameLength");
        checkPositiveOrZero(initialBytesToStrip, "initialBytesToStrip");
        this.lengthFieldOffsetGetter = checkNotNull(lengthFieldOffsetGetter, "lengthFieldOffsetGetter");
        this.maxFrameLength = maxFrameLength;
        this.lengthFieldLength = lengthFieldLength;
        this.lengthFieldLengthGetter = IntTool.getInstance(lengthFieldLength);
        this.lengthAdjustment = lengthAdjustment;
        this.initialBytesToStrip = initialBytesToStrip;
        this.failFast = failFast;
    }

    @Override
    protected final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        Object decoded = decode(ctx, in);
        if (decoded != null) {
            out.add(decoded);
        }
    }

    private void discardingTooLongFrame(ByteBuf in) {
        long bytesToDiscard = this.bytesToDiscard;
        int localBytesToDiscard = (int) Math.min(bytesToDiscard, in.readableBytes());
        in.skipBytes(localBytesToDiscard);
        bytesToDiscard -= localBytesToDiscard;
        this.bytesToDiscard = bytesToDiscard;

        failIfNecessary(false);
    }

    private static void failOnNegativeLengthField(ByteBuf in, long frameLength, int lengthFieldEndOffset) {
        in.skipBytes(lengthFieldEndOffset);
        throw new CorruptedFrameException("negative pre-adjustment length field: " + frameLength);
    }

    private static void failOnFrameLengthLessThanLengthFieldEndOffset(ByteBuf in, long frameLength, int lengthFieldEndOffset) {
        in.skipBytes(lengthFieldEndOffset);
        throw new CorruptedFrameException("Adjusted frame length (" + frameLength + ") is less than lengthFieldEndOffset: " + lengthFieldEndOffset);
    }

    private void exceededFrameLength(ByteBuf in, long frameLength) {
        long discard = frameLength - in.readableBytes();
        tooLongFrameLength = frameLength;

        if (discard < 0) {
            // buffer contains more bytes then the frameLength so we can discard all now
            in.skipBytes((int) frameLength);
        } else {
            // Enter the discard mode and discard everything received so far.
            discardingTooLongFrame = true;
            bytesToDiscard = discard;
            in.skipBytes(in.readableBytes());
        }
        failIfNecessary(true);
    }

    private static void failOnFrameLengthLessThanInitialBytesToStrip(ByteBuf in, long frameLength, int initialBytesToStrip) {
        in.skipBytes((int) frameLength);
        throw new CorruptedFrameException("Adjusted frame length (" + frameLength + ") is less than initialBytesToStrip: " + initialBytesToStrip);
    }

    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) {
        long frameLength = 0;
        if (frameLengthInt == -1) { // new frame

            if (discardingTooLongFrame) {
                discardingTooLongFrame(in);
            }

            int lengthFieldOffset = lengthFieldOffsetGetter.applyAsInt(in);
            int lengthFieldEndOffset = lengthFieldOffset + lengthFieldLength;

            if (in.readableBytes() < lengthFieldEndOffset) {
                return null;
            }

            int actualLengthFieldOffset = in.readerIndex() + lengthFieldOffset;
            frameLength = lengthFieldLengthGetter.get(in, actualLengthFieldOffset);

            if (frameLength < 0) {
                failOnNegativeLengthField(in, frameLength, lengthFieldEndOffset);
            }

            frameLength += lengthAdjustment + lengthFieldEndOffset;

            if (frameLength < lengthFieldEndOffset) {
                failOnFrameLengthLessThanLengthFieldEndOffset(in, frameLength, lengthFieldEndOffset);
            }

            if (frameLength > maxFrameLength) {
                exceededFrameLength(in, frameLength);
                return null;
            }
            // never overflows because it's less than maxFrameLength
            frameLengthInt = (int) frameLength;
        }
        if (in.readableBytes() < frameLengthInt) { // frameLengthInt exist , just check buf
            return null;
        }
        if (initialBytesToStrip > frameLengthInt) {
            failOnFrameLengthLessThanInitialBytesToStrip(in, frameLength, initialBytesToStrip);
        }
        in.skipBytes(initialBytesToStrip);

        // extract frame
        int readerIndex = in.readerIndex();
        int actualFrameLength = frameLengthInt - initialBytesToStrip;
        ByteBuf frame = extractFrame(ctx, in, readerIndex, actualFrameLength);
        in.readerIndex(readerIndex + actualFrameLength);
        frameLengthInt = -1; // start processing the next frame
        return frame;
    }

    private void failIfNecessary(boolean firstDetectionOfTooLongFrame) {
        if (bytesToDiscard == 0) {
            // Reset to the initial state and tell the handlers that
            // the frame was too large.
            long tooLongFrameLength = this.tooLongFrameLength;
            this.tooLongFrameLength = 0;
            discardingTooLongFrame = false;
            if (!failFast || firstDetectionOfTooLongFrame) {
                fail(tooLongFrameLength);
            }
        } else {
            // Keep discarding and notify handlers if necessary.
            if (failFast && firstDetectionOfTooLongFrame) {
                fail(tooLongFrameLength);
            }
        }
    }

    protected ByteBuf extractFrame(ChannelHandlerContext ctx, ByteBuf buffer, int index, int length) {
        return buffer.retainedSlice(index, length);
    }

    private void fail(long frameLength) {
        if (frameLength > 0) {
            throw new TooLongFrameException("Adjusted frame length exceeds " + maxFrameLength + ": " + frameLength + " - discarded");
        } else {
            throw new TooLongFrameException("Adjusted frame length exceeds " + maxFrameLength + " - discarding");
        }
    }
}
