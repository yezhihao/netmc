package io.github.yezhihao.netmc;

import io.github.yezhihao.netmc.handler.*;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public class TCPServer extends Server {

    protected TCPServer(NettyConfig config) {
        super(config);
    }

    protected AbstractBootstrap initialize() {
        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory(config.name, Thread.MAX_PRIORITY));
        workerGroup = new NioEventLoopGroup(config.workerCore, new DefaultThreadFactory(config.name, Thread.MAX_PRIORITY));
        if (config.businessCore > 0)
            businessGroup = new ThreadPoolExecutor(config.businessCore, config.businessCore, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new DefaultThreadFactory(config.name + "-B", true, Thread.NORM_PRIORITY));
        return new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(NioChannelOption.SO_REUSEADDR, true)
                .option(NioChannelOption.SO_BACKLOG, 1024)
                .childOption(NioChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {

                    private final TCPMessageAdapter adapter = new TCPMessageAdapter(config.sessionManager);
                    private final MessageDecoderWrapper decoder = new MessageDecoderWrapper(config.decoder);
                    private final MessageEncoderWrapper encoder = new MessageEncoderWrapper(config.encoder);
                    private final DispatcherHandler dispatcher = new DispatcherHandler(config.handlerMapping, config.handlerInterceptor, businessGroup);

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
    }

    private ByteToMessageDecoder frameDecoder() {
        if (config.lengthField != null) {
            if (config.delimiters != null) {
                return new LengthFieldAndDelimiterFrameDecoder(config.maxFrameLength, config.lengthField, config.delimiters);
            } else {
                return new LengthFieldBasedFrameDecoder(config.maxFrameLength,
                        config.lengthField.lengthFieldOffset, config.lengthField.lengthFieldLength,
                        config.lengthField.lengthAdjustment, config.lengthField.initialBytesToStrip);
            }
        }
        return new DelimiterBasedFrameDecoder(config.maxFrameLength, config.delimiters);
    }
}