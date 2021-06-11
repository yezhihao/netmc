package io.github.yezhihao.netmc.model;

public class MyHeader {

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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public int getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(int serialNo) {
        this.serialNo = serialNo;
    }
}