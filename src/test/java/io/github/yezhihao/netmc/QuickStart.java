package io.github.yezhihao.netmc;

import io.github.yezhihao.netmc.core.DefaultHandlerMapping;
import io.github.yezhihao.netmc.endpoint.MyHandlerInterceptor;
import io.github.yezhihao.netmc.codec.MyMessageDecoder;
import io.github.yezhihao.netmc.codec.MyMessageEncoder;
import io.github.yezhihao.netmc.session.SessionManager;

import java.nio.charset.StandardCharsets;

public class QuickStart {

    public static void main(String[] args) {
        NettyConfig jtConfig = new NettyConfig.Builder()
                .setPort(8080)
                .setMaxFrameLength(1024)
                .setDelimiters(new byte[][]{"|".getBytes(StandardCharsets.UTF_8)})
                .setDecoder(new MyMessageDecoder())
                .setEncoder(new MyMessageEncoder())
                .setHandlerMapping(new DefaultHandlerMapping("io.github.yezhihao.netmc.endpoint"))
//                .setHandlerMapping(new SpringHandlerMapping("org.yzh.web.endpoint"))
                .setHandlerInterceptor(new MyHandlerInterceptor())
                .setSessionManager(new SessionManager())
                .build();

        TCPServer tcpServer = new TCPServer("Test服务", jtConfig);
        tcpServer.start();
    }
}