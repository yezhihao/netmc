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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(int serialNo) {
        this.serialNo = serialNo;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("clientId='").append(clientId).append('\'');
        sb.append(", type=").append(type);
        sb.append(", serialNo=").append(serialNo);
        sb.append('}');
        return sb.toString();
    }
}