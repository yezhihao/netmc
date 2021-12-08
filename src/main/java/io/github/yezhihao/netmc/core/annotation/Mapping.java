package io.github.yezhihao.netmc.core.annotation;

import java.lang.annotation.*;

/**
 * 消息类型映射
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Mapping {

    int[] types();

    String desc() default "";

}