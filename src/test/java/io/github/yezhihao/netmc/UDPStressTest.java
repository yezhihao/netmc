package io.github.yezhihao.netmc;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 压力测试
 * 模拟1200台设备，每100毫秒上报一次位置信息
 */
public class UDPStressTest {

    private static final byte[] bytes = "|123,1,123;testtesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttesttest|".getBytes(StandardCharsets.UTF_8);

    private static final AtomicInteger count = new AtomicInteger();
    //连接数量
    private static final int size = 1000;
    //上报间隔(毫秒)
    private static final long Interval = 10;

    public static void main(String[] args) throws Exception {
        SocketAddress target = new InetSocketAddress("127.0.0.1", 7611);
        DatagramSocket[] clients = new DatagramSocket[size];
        for (int i = 0; i < size; i++) {
            clients[i] = new DatagramSocket(40001 + i);
        }

        long start = System.currentTimeMillis();
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000L);
                } catch (Exception e) {
                }
                int num = count.get();
                long time = (System.currentTimeMillis() - start) / 1000;
                System.out.println(time + "\t" + num + "\t" + num / time);
            }
        });
        t.setName(Thread.currentThread().getName() + "-c");
        t.setPriority(Thread.MIN_PRIORITY);
        t.setDaemon(true);
        t.start();

        while (true) {
            for (int i = 0; i < size; i++) {
                count.addAndGet(1);
                clients[i].send(new DatagramPacket(bytes, bytes.length, target));
            }
            Thread.sleep(Interval);
        }
    }
}