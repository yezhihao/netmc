package io.github.yezhihao.netmc.core.annotation;

import java.lang.annotation.*;

/**
 * 异步批处理消息注解
 * @author yezhihao
 * home https://gitee.com/yezhihao/jt808-server
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AsyncBatch {

    /** 线程数量 */
    int poolSize() default 2;

    /** 最大累计消息数 */
    int maxElements() default 4000;

    /** 最大等待时间 */
    int maxWait() default 1000;

}