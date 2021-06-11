package io.github.yezhihao.netmc.core.model;

import java.io.Serializable;

/**
 * 消息体
 * @author yezhihao
 * home https://gitee.com/yezhihao/jt808-server
 */
public interface Message extends Serializable {

    /** 客户端唯一标识 */
    Serializable getClientId();

    /** 消息类型 */
    Serializable getMessageId();

    /** 消息类型(日志输出) */
    String getMessageName();

    /** 消息流水号 */
    int getSerialNo();
}