package io.github.yezhihao.netmc.core;

import io.github.yezhihao.netmc.core.annotation.Async;
import io.github.yezhihao.netmc.core.annotation.AsyncBatch;
import io.github.yezhihao.netmc.core.annotation.Mapping;
import io.github.yezhihao.netmc.core.handler.AsyncBatchHandler;
import io.github.yezhihao.netmc.core.handler.Handler;
import io.github.yezhihao.netmc.core.handler.SimpleHandler;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息处理映射
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public abstract class AbstractHandlerMapping implements HandlerMapping {

    private final Map<Object, Handler> handlerMap = new HashMap<>(64);

    /**
     * 将Endpoint中被@Mapping标记的方法注册到映射表
     */
    protected synchronized void registerHandlers(Object bean) {
        Class<?> beanClass = bean.getClass();
        Method[] methods = beanClass.getDeclaredMethods();

        for (Method method : methods) {

            Mapping mapping = method.getAnnotation(Mapping.class);
            if (mapping != null) {

                String desc = mapping.desc();
                int[] types = mapping.types();

                AsyncBatch asyncBatch = method.getAnnotation(AsyncBatch.class);
                Handler handler;

                if (asyncBatch != null) {
                    handler = new AsyncBatchHandler(bean, method, desc, asyncBatch.poolSize(), asyncBatch.maxElements(), asyncBatch.maxWait());

                } else {
                    handler = new SimpleHandler(bean, method, desc, method.isAnnotationPresent(Async.class));
                }

                for (int type : types) {
                    handlerMap.put(type, handler);
                }
            }
        }
    }

    /**
     * 根据消息类型获取Handler
     */
    public Handler getHandler(int messageId) {
        return handlerMap.get(messageId);
    }
}