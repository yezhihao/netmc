package io.github.yezhihao.netmc;

import io.github.yezhihao.netmc.codec.DelimiterBasedFrameDecoder;
import io.github.yezhihao.netmc.codec.LengthFieldAndDelimiterFrameDecoder;
import io.github.yezhihao.netmc.codec.MessageDecoderWrapper;
import io.github.yezhihao.netmc.codec.MessageEncoderWrapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public class TCPServer implements Server {

    private static final Logger log = LoggerFactory.getLogger(TCPServer.class);
    private boolean isRunning;
    private final NettyConfig config;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ExecutorService businessGroup;

    protected TCPServer(NettyConfig config) {
        this.config = config;
    }

    private ByteToMessageDecoder frameDecoder() {
        if (config.lengthField == null)
            return new DelimiterBasedFrameDecoder(config.maxFrameLength, config.delimiters);
        return new LengthFieldAndDelimiterFrameDecoder(config.maxFrameLength, config.lengthField, config.delimiters);
    }

    private DispatcherHandler dispatcherHandler() {
        if (businessGroup == null)
            return new DispatcherHandler(config.handlerMapping, config.handlerInterceptor);
        return new AsyncDispatcherHandler(config.handlerMapping, config.handlerInterceptor, businessGroup);
    }

    private boolean startInternal() {
        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory(config.name, Thread.MAX_PRIORITY));
        workerGroup = new NioEventLoopGroup(config.workerCore, new DefaultThreadFactory(config.name, Thread.MAX_PRIORITY));
        if (config.businessCore > 0)
            businessGroup = new ThreadPoolExecutor(config.businessCore, config.businessCore, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new DefaultThreadFactory(config.name + "-B", true, Thread.NORM_PRIORITY));
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(NioChannelOption.SO_REUSEADDR, true)
                .option(NioChannelOption.SO_BACKLOG, 1024)
                .childOption(NioChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {

                    private final TCPMessageAdapter adapter = new TCPMessageAdapter(config.sessionManager);
                    private final MessageDecoderWrapper decoder = new MessageDecoderWrapper(config.decoder);
                    private final MessageEncoderWrapper encoder = new MessageEncoderWrapper(config.encoder);
                    private final DispatcherHandler dispatcher = dispatcherHandler();

                    @Override
                    public void initChannel(NioSocketChannel channel) {
                        channel.pipeline()
                                .addLast(new IdleStateHandler(config.readerIdleTime, config.writerIdleTime, config.allIdleTime))
                                .addLast("frameDecoder", frameDecoder())
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
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        if (businessGroup != null)
            businessGroup.shutdown();
        log.warn("==={}已经停止,port:{}===", config.name, config.port);
    }
}