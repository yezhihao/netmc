package io.github.yezhihao.netmc.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public class Stopwatch {

    private final AtomicInteger count = new AtomicInteger();
    private final Thread thread;

    public Stopwatch start() {
        this.thread.start();
        return this;
    }

    public int increment() {
        return count.incrementAndGet();
    }

    public Stopwatch() {
        thread = new Thread(() -> {
            long start;
            while (true) {
                if (count.get() > 0) {
                    start = System.currentTimeMillis();
                    break;
                }
                try {
                    Thread.sleep(1L);
                } catch (Exception ignored) {
                }
            }
            while (true) {
                try {
                    Thread.sleep(2000L);
                } catch (Exception ignored) {
                }
                int num = count.get();
                long time = (System.currentTimeMillis() - start) / 1000;
                System.out.println(time + "\t" + num + "\t" + num / time);
            }
        });
        thread.setName(Thread.currentThread().getName() + "-c");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
    }
}
