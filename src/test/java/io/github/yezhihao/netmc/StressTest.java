package io.github.yezhihao.netmc;

import io.github.yezhihao.netmc.util.Client;
import io.github.yezhihao.netmc.util.Stopwatch;

import java.nio.charset.StandardCharsets;

/**
 * 压力测试
 * 模拟1200台设备，每100毫秒上报一次位置信息
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public class StressTest {

    private static final byte[] bytes = "|123,1,123;testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest|".getBytes(StandardCharsets.UTF_8);
    private static final Stopwatch STOPWATCH = new Stopwatch().start();

    public static final String host = "192.168.1.2";
    public static final int port = QuickStart.port;

    private static final int size = 1200;
    private static final long Interval = 100;

    public static void main(String[] args) throws Exception {
        Client[] udps = null;
        Client[] tcps = null;
        udps = Client.UDP(host, port, size);
        tcps = Client.TCP(host, port, size);

        while (true) {
            for (int i = 0; i < size; i++) {
                if (udps != null) {
                    udps[i].send(bytes);
                    STOPWATCH.increment();
                }
                if (tcps != null) {
                    tcps[i].send(bytes);
                    STOPWATCH.increment();
                }
            }
            Thread.sleep(Interval);
        }
    }
}