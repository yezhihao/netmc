package io.github.yezhihao.netmc;

import io.netty.buffer.ByteBufUtil;

import java.nio.charset.StandardCharsets;

public class Test {
    public static void main(String[] args) {
        //测试代码的消息结构“|客户端ID,消息类型,消息流水号;消息体|”
        byte[] bytes = "|123,1,123;test|".getBytes(StandardCharsets.UTF_8);
        System.out.println(ByteBufUtil.hexDump(bytes));
    }
}
