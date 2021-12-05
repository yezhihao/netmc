package io.github.yezhihao.netmc;

import io.github.yezhihao.netmc.codec.MessageDecoderWrapper;
import io.github.yezhihao.netmc.codec.MessageEncoderWrapper;
import io.github.yezhihao.netmc.codec.UDPDelimiterBasedFrameDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public class UDPServer implements Server {

    private static final Logger log = LoggerFactory.getLogger(UDPServer.class);
    private boolean isRunning;
    private final NettyConfig config;
    private EventLoopGroup workerGroup;

    protected UDPServer(NettyConfig config) {
        this.config = config;
    }

    private boolean startInternal() {
        workerGroup = new NioEventLoopGroup(new DefaultThreadFactory(config.name, Thread.MAX_PRIORITY));
        Bootstrap bootstrap = new Bootstrap()
                .group(workerGroup)
                .channel(NioDatagramChannel.class)
                .option(NioChannelOption.SO_REUSEADDR, true)
                .handler(new ChannelInitializer<NioDatagramChannel>() {

                    private final UDPMessageAdapter adapter = config.delimiters == null ?
                            new UDPMessageAdapter(config.sessionManager, config.readerIdleTime) :
                            new UDPDelimiterBasedFrameDecoder(config.sessionManager, config.readerIdleTime, config.delimiters);
                    private final MessageDecoderWrapper decoder = new MessageDecoderWrapper(config.decoder);
                    private final MessageEncoderWrapper encoder = new MessageEncoderWrapper(config.encoder);
                    private final DispatcherHandler dispatcher = new DispatcherHandler(config.handlerMapping, config.handlerInterceptor);

                    @Override
                    public void initChannel(NioDatagramChannel channel) {
                        channel.pipeline()
                                .addLast("adapter", adapter)
                                .addLast("decoder", decoder)
                                .addLast("encoder", encoder)
                                .addLast("dispatcher", dispatcher);
                    }
                });

        ChannelFuture future = bootstrap.bind(config.port).awaitUninterruptibly();
        future.channel().closeFuture().addListener(f -> {
            if (isRunning) stop();
        });
        if (future.cause() != null)
            log.error("启动失败", future.cause());
        return future.isSuccess();
    }

    public synchronized boolean start() {
        if (isRunning) {
            log.warn("==={}已经启动,port:{}===", config.name, config.port);
            return isRunning;
        }
        if (isRunning = startInternal())
            log.warn("==={}启动成功,port:{}===", config.name, config.port);
        return isRunning;
    }

    public synchronized void stop() {
        isRunning = false;
        workerGroup.shutdownGracefully();
        log.warn("==={}已经停止,port:{}===", config.name, config.port);
    }
}