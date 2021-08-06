package io.github.yezhihao.netmc;

import io.github.yezhihao.netmc.codec.Delimiter;
import io.github.yezhihao.netmc.codec.LengthField;
import io.github.yezhihao.netmc.codec.MessageDecoder;
import io.github.yezhihao.netmc.codec.MessageEncoder;
import io.github.yezhihao.netmc.core.HandlerInterceptor;
import io.github.yezhihao.netmc.core.HandlerMapping;
import io.github.yezhihao.netmc.session.SessionListener;
import io.github.yezhihao.netmc.session.SessionManager;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author yezhihao
 * home https://gitee.com/yezhihao/jt808-server
 */
public class NettyConfig {

    protected final int readerIdleTime;
    protected final int writerIdleTime;
    protected final int allIdleTime;
    protected final int port;
    protected final int maxFrameLength;
    protected final LengthField lengthField;
    protected final Delimiter[] delimiter;
    protected final MessageDecoder decoder;
    protected final MessageEncoder encoder;
    protected final ChannelInboundHandlerAdapter adapter;
    protected final HandlerMapping handlerMapping;
    protected final HandlerInterceptor handlerInterceptor;
    protected final SessionManager sessionManager;
    protected final SessionListener sessionListener;

    private NettyConfig(int readerIdleTime,
                        int writerIdleTime,
                        int allIdleTime,
                        int port,
                        int maxFrameLength,
                        LengthField lengthField,
                        Delimiter[] delimiter,
                        MessageDecoder decoder,
                        MessageEncoder encoder,
                        HandlerMapping handlerMapping,
                        HandlerInterceptor handlerInterceptor,
                        SessionManager sessionManager,
                        SessionListener sessionListener
    ) {
        this.readerIdleTime = readerIdleTime;
        this.writerIdleTime = writerIdleTime;
        this.allIdleTime = allIdleTime;
        this.port = port;
        this.maxFrameLength = maxFrameLength;
        this.lengthField = lengthField;
        this.delimiter = delimiter;
        this.decoder = decoder;
        this.encoder = encoder;
        this.handlerMapping = handlerMapping;
        this.handlerInterceptor = handlerInterceptor;
        this.sessionManager = sessionManager;
        this.sessionListener = sessionListener;
        this.adapter = new TCPServerHandler(this.handlerMapping, this.handlerInterceptor, this.sessionManager, this.sessionListener);
    }

    public static NettyConfig.Builder custom() {
        return new Builder();
    }

    public static class Builder {

        private int readerIdleTime = 240;
        private int writerIdleTime = 0;
        private int allIdleTime = 0;
        private int port;
        private int maxFrameLength;
        private LengthField lengthField;
        private Delimiter[] delimiters;
        private MessageDecoder decoder;
        private MessageEncoder encoder;
        private HandlerMapping handlerMapping;
        private HandlerInterceptor handlerInterceptor;
        private SessionManager sessionManager;
        private SessionListener sessionListener;

        public Builder() {
        }

        public Builder setIdleStateTime(int readerIdleTime, int writerIdleTime, int allIdleTime) {
            this.readerIdleTime = readerIdleTime;
            this.writerIdleTime = writerIdleTime;
            this.allIdleTime = allIdleTime;
            return this;
        }

        public Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public Builder setMaxFrameLength(int maxFrameLength) {
            this.maxFrameLength = maxFrameLength;
            return this;
        }

        public Builder setLengthField(LengthField lengthField) {
            this.lengthField = lengthField;
            return this;
        }

        public Builder setDelimiters(byte[][] delimiters) {
            Delimiter[] t = new Delimiter[delimiters.length];
            for (int i = 0; i < delimiters.length; i++) {
                t[i] = new Delimiter(delimiters[i]);
            }
            this.delimiters = t;
            return this;
        }

        public Builder setDelimiters(Delimiter... delimiters) {
            this.delimiters = delimiters;
            return this;
        }

        public Builder setDecoder(MessageDecoder decoder) {
            this.decoder = decoder;
            return this;
        }

        public Builder setEncoder(MessageEncoder encoder) {
            this.encoder = encoder;
            return this;
        }

        public Builder setHandlerMapping(HandlerMapping handlerMapping) {
            this.handlerMapping = handlerMapping;
            return this;
        }

        public Builder setHandlerInterceptor(HandlerInterceptor handlerInterceptor) {
            this.handlerInterceptor = handlerInterceptor;
            return this;
        }

        public Builder setSessionManager(SessionManager sessionManager) {
            this.sessionManager = sessionManager;
            return this;
        }

        public Builder setSessionListener(SessionListener sessionListener) {
            this.sessionListener = sessionListener;
            return this;
        }

        public NettyConfig build() {
            return new NettyConfig(
                    this.readerIdleTime,
                    this.writerIdleTime,
                    this.allIdleTime,
                    this.port,
                    this.maxFrameLength,
                    this.lengthField,
                    this.delimiters,
                    this.decoder,
                    this.encoder,
                    this.handlerMapping,
                    this.handlerInterceptor,
                    this.sessionManager,
                    this.sessionListener
            );
        }
    }
}