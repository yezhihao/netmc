package io.github.yezhihao.netmc.core.model;

import java.io.Serializable;

/**
 * 消息体
 * @author yezhihao
 * home https://gitee.com/yezhihao/jt808-server
 */
public interface Message<T extends Header> extends Serializable {

    T getHeader();

    Object getMessageType();
}