package io.github.yezhihao.netmc.codec;

import io.netty.buffer.ByteBuf;
import io.github.yezhihao.netmc.session.Session;

/**
 * 基础消息解码
 * @author yezhihao
 * home https://gitee.com/yezhihao/jt808-server
 */
public interface MessageDecoder<T> {

    T decode(ByteBuf buf);

    T decode(ByteBuf buf, Session session);

}