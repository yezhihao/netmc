package io.github.yezhihao.netmc.model;

import io.github.yezhihao.netmc.core.model.Header;

public class MyHeader implements Header<String, Integer> {

    /** 客户端ID */
    private String clientId;
    /** 消息类型 */
    private int type;
    /** 消息流水号 */
    private int serialNo;

    public MyHeader() {
    }

    public MyHeader(int type, String clientId, int serialNo) {
        this.type = type;
        this.clientId = clientId;
        this.serialNo = serialNo;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    @Override
    public int getSerialNo() {
        return serialNo;
    }

    @Override
    public void setSerialNo(int serialNo) {
        this.serialNo = serialNo;
    }
}