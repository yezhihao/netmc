package io.github.yezhihao.netmc.core.model;

import java.io.Serializable;

/**
 * 消息体
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public interface Message extends Serializable {

    /** 客户端唯一标识 */
    String getClientId();

    /** 消息类型 */
    int getMessageId();

    /** 消息流水号 */
    int getSerialNo();
}