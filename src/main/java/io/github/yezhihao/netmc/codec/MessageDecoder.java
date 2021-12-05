package io.github.yezhihao.netmc.codec;

import io.github.yezhihao.netmc.core.model.Message;
import io.github.yezhihao.netmc.session.Session;
import io.netty.buffer.ByteBuf;

/**
 * 基础消息解码
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public interface MessageDecoder<T extends Message> {

    T decode(ByteBuf buf, Session session);

}