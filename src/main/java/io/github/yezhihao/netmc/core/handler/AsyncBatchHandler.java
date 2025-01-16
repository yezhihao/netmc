package io.github.yezhihao.netmc.core.handler;

import io.github.yezhihao.netmc.core.model.Message;
import io.github.yezhihao.netmc.session.Session;
import io.github.yezhihao.netmc.util.VirtualList;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步批量处理
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public class AsyncBatchHandler extends Handler {

    private static final Logger log = LoggerFactory.getLogger(AsyncBatchHandler.class);

    private final ConcurrentLinkedQueue<Message> queue;

    private final ExecutorService executor;

    private final int poolSize;

    private final int maxElements;

    private final int maxWait;

    private final int warningLines;

    public AsyncBatchHandler(Object actionClass, Method actionMethod, String desc, int poolSize, int maxElements, int maxWait) {
        super(actionClass, actionMethod, desc);

        Class<?>[] parameterTypes = actionMethod.getParameterTypes();
        if (parameterTypes.length > 1)
            throw new RuntimeException("@AsyncBatch方法仅支持一个List参数:" + actionMethod);
        if (!parameterTypes[0].isAssignableFrom(List.class))
            throw new RuntimeException("@AsyncBatch方法的参数不是List类型:" + actionMethod);

        this.poolSize = poolSize;
        this.maxElements = maxElements;
        this.maxWait = maxWait;
        this.warningLines = maxElements * poolSize * 50;

        this.queue = new ConcurrentLinkedQueue<>();
        this.executor = Executors.newFixedThreadPool(this.poolSize, new DefaultThreadFactory(actionMethod.getName(), true, Thread.NORM_PRIORITY));

        for (int i = 0; i < poolSize; i++) {
            boolean master = i == 0;
            executor.execute(() -> {
                try {
                    startInternal(master);
                } catch (Exception e) {
                    log.error("批处理线程出错", e);
                }
            });
        }
    }

    public <T extends Message> T invoke(T request, Session session) {
        queue.offer(request);
        return null;
    }

    public void startInternal(boolean master) {
        Message[] array = new Message[maxElements];
        long logtime = 0;
        long starttime = 0;

        for (; ; ) {
            Message temp;
            int i = 0;
            while ((temp = queue.poll()) != null) {
                array[i++] = temp;
                if (i >= maxElements)
                    break;
            }

            if (i > 0) {
                starttime = System.currentTimeMillis();
                try {
                    targetMethod.invoke(targetObject, new VirtualList<>(array, i));
                } catch (InvocationTargetException e) {
                    log.error(targetMethod.getName(), e.getTargetException());
                } catch (Exception e) {
                    log.error(targetMethod.getName(), e);
                }
                long time = System.currentTimeMillis() - starttime;
                if (time > 1000L)
                    log.warn("批处理耗时:{}ms,共{}条记录", time, i);
            }

            if (i < maxElements) {
                try {
                    Arrays.fill(array, null);
                    Thread.sleep(maxWait);
                } catch (InterruptedException ignored) {
                }
            } else if (master) {
                if (logtime < starttime) {
                    logtime = starttime + 5000L;

                    int size = queue.size();
                    if (size > warningLines) {
                        log.warn("批处理队列繁忙, size:{}", size);
                    }
                }
            }
        }
    }
}