package io.github.yezhihao.netmc.core.annotation;

import java.lang.annotation.*;

/**
 * 消息接入点
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Endpoint {

}