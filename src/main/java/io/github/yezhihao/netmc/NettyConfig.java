package io.github.yezhihao.netmc;

import io.github.yezhihao.netmc.codec.Delimiter;
import io.github.yezhihao.netmc.codec.LengthField;
import io.github.yezhihao.netmc.codec.MessageDecoder;
import io.github.yezhihao.netmc.codec.MessageEncoder;
import io.github.yezhihao.netmc.core.HandlerInterceptor;
import io.github.yezhihao.netmc.core.HandlerMapping;
import io.github.yezhihao.netmc.session.SessionManager;
import io.netty.util.NettyRuntime;
import io.netty.util.internal.ObjectUtil;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public class NettyConfig {

    protected final int workerCore;
    protected final int businessCore;
    protected final int readerIdleTime;
    protected final int writerIdleTime;
    protected final int allIdleTime;
    protected final Integer port;
    protected final Integer maxFrameLength;
    protected final LengthField lengthField;
    protected final Delimiter[] delimiters;
    protected final MessageDecoder decoder;
    protected final MessageEncoder encoder;
    protected final HandlerMapping handlerMapping;
    protected final HandlerInterceptor handlerInterceptor;
    protected final SessionManager sessionManager;
    protected final boolean enableUDP;
    protected final Server server;
    protected final String name;

    private NettyConfig(int workerGroup,
                        int businessGroup,
                        int readerIdleTime,
                        int writerIdleTime,
                        int allIdleTime,
                        Integer port,
                        Integer maxFrameLength,
                        LengthField lengthField,
                        Delimiter[] delimiters,
                        MessageDecoder decoder,
                        MessageEncoder encoder,
                        HandlerMapping handlerMapping,
                        HandlerInterceptor handlerInterceptor,
                        SessionManager sessionManager,
                        boolean enableUDP,
                        String name
    ) {
        ObjectUtil.checkNotNull(port, "port");
        ObjectUtil.checkPositive(port, "port");
        ObjectUtil.checkNotNull(decoder, "decoder");
        ObjectUtil.checkNotNull(encoder, "encoder");
        ObjectUtil.checkNotNull(handlerMapping, "handlerMapping");
        ObjectUtil.checkNotNull(handlerInterceptor, "handlerInterceptor");
        if (!enableUDP) {
            ObjectUtil.checkNotNull(maxFrameLength, "maxFrameLength");
            ObjectUtil.checkPositive(maxFrameLength, "maxFrameLength");
            if (delimiters == null && lengthField == null) {
                throw new IllegalArgumentException("At least one of delimiters and lengthField is not empty");
            }
        }

        int processors = NettyRuntime.availableProcessors();
        this.workerCore = workerGroup > 0 ? workerGroup : processors + 2;
        this.businessCore = businessGroup > 0 ? businessGroup : Math.max(1, processors >> 1);
        this.readerIdleTime = readerIdleTime;
        this.writerIdleTime = writerIdleTime;
        this.allIdleTime = allIdleTime;
        this.port = port;
        this.maxFrameLength = maxFrameLength;
        this.lengthField = lengthField;
        this.delimiters = delimiters;
        this.decoder = decoder;
        this.encoder = encoder;
        this.handlerMapping = handlerMapping;
        this.handlerInterceptor = handlerInterceptor;
        this.sessionManager = sessionManager != null ? sessionManager : new SessionManager();
        this.enableUDP = enableUDP;

        if (enableUDP) {
            this.name = name != null ? name : "UDP";
            this.server = new UDPServer(this);
        } else {
            this.name = name != null ? name : "TCP";
            this.server = new TCPServer(this);
        }
    }

    public Server build() {
        return server;
    }

    public static NettyConfig.Builder custom() {
        return new Builder();
    }

    public static class Builder {

        private int workerCore;
        private int businessCore;
        private int readerIdleTime = 240;
        private int writerIdleTime = 0;
        private int allIdleTime = 0;
        private Integer port;
        private Integer maxFrameLength;
        private LengthField lengthField;
        private Delimiter[] delimiters;
        private MessageDecoder decoder;
        private MessageEncoder encoder;
        private HandlerMapping handlerMapping;
        private HandlerInterceptor handlerInterceptor;
        private SessionManager sessionManager;
        private boolean enableUDP;
        private String name;

        public Builder() {
        }

        public Builder setThreadGroup(int workerCore, int businessCore) {
            this.workerCore = workerCore;
            this.businessCore = businessCore;
            return this;
        }

        public Builder setIdleStateTime(int readerIdleTime, int writerIdleTime, int allIdleTime) {
            this.readerIdleTime = readerIdleTime;
            this.writerIdleTime = writerIdleTime;
            this.allIdleTime = allIdleTime;
            return this;
        }

        public Builder setPort(Integer port) {
            this.port = port;
            return this;
        }

        public Builder setMaxFrameLength(Integer maxFrameLength) {
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

        public Builder setEnableUDP(boolean enableUDP) {
            this.enableUDP = enableUDP;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Server build() {
            return new NettyConfig(
                    this.workerCore,
                    this.businessCore,
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
                    this.enableUDP,
                    this.name
            ).build();
        }
    }
}