package io.github.yezhihao.netmc;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public abstract class Server {

    protected static final Logger log = LoggerFactory.getLogger(Server.class);
    protected boolean isRunning;
    protected final NettyConfig config;
    protected EventLoopGroup bossGroup;
    protected EventLoopGroup workerGroup;
    protected ExecutorService businessGroup;

    protected Server(NettyConfig config) {
        this.config = config;
    }

    protected abstract AbstractBootstrap initialize();

    public synchronized boolean start() {
        if (isRunning) {
            log.warn("==={}已经启动,port:{}===", config.name, config.port);
            return isRunning;
        }

        AbstractBootstrap bootstrap = initialize();
        ChannelFuture future = bootstrap.bind(config.port).awaitUninterruptibly();
        future.channel().closeFuture().addListener(f -> {
            if (isRunning) stop();
        });
        if (future.cause() != null)
            log.error("启动失败", future.cause());

        if (isRunning = future.isSuccess())
            log.warn("==={}启动成功,port:{}===", config.name, config.port);
        return isRunning;
    }

    public synchronized void stop() {
        isRunning = false;
        bossGroup.shutdownGracefully();
        if (workerGroup != null)
            workerGroup.shutdownGracefully();
        if (businessGroup != null)
            businessGroup.shutdown();
        log.warn("==={}已经停止,port:{}===", config.name, config.port);
    }
}