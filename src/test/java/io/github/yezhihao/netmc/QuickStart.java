package io.github.yezhihao.netmc;

import io.github.yezhihao.netmc.codec.MyMessageDecoder;
import io.github.yezhihao.netmc.codec.MyMessageEncoder;
import io.github.yezhihao.netmc.core.DefaultHandlerMapping;
import io.github.yezhihao.netmc.endpoint.MyHandlerInterceptor;
import io.github.yezhihao.netmc.session.SessionManager;

import java.nio.charset.StandardCharsets;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public class QuickStart {

    public static void main(String[] args) {
        Server udpServer = new NettyConfig.Builder()
                .setPort(7611)
                .setDelimiters(new byte[][]{"|".getBytes(StandardCharsets.UTF_8)})
                .setDecoder(new MyMessageDecoder())
                .setEncoder(new MyMessageEncoder())
                .setHandlerMapping(new DefaultHandlerMapping("io.github.yezhihao.netmc.endpoint"))
                .setHandlerInterceptor(new MyHandlerInterceptor())
                .setSessionManager(new SessionManager())
                .setEnableUDP(true)
                .build();
        udpServer.start();

        Server tcpServer = new NettyConfig.Builder()
                .setPort(7611)
                .setMaxFrameLength(1024)
                .setDelimiters(new byte[][]{"|".getBytes(StandardCharsets.UTF_8)})
                .setDecoder(new MyMessageDecoder())
                .setEncoder(new MyMessageEncoder())
                .setHandlerMapping(new DefaultHandlerMapping("io.github.yezhihao.netmc.endpoint"))
                .setHandlerInterceptor(new MyHandlerInterceptor())
                .setSessionManager(new SessionManager())
                .build();
        tcpServer.start();
    }
}