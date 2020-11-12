package io.github.yezhihao.netmc.core.model;

import java.io.Serializable;

/**
 * 消息头
 * @author yezhihao
 * home https://gitee.com/yezhihao/jt808-server
 */
public interface Header<ClientID, Type> extends Serializable {

    /** 客户端唯一标识 */
    ClientID getClientId();

    /** 消息类型 */
    Type getType();

    /** 消息流水号 */
    int getSerialNo();

    void setSerialNo(int serialNo);
}