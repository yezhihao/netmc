package io.github.yezhihao.netmc.codec;

import io.github.yezhihao.netmc.session.Session;
import io.netty.buffer.ByteBuf;

/**
 * 基础消息编码
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public interface MessageEncoder<T> {

    ByteBuf encode(T message, Session session);

}