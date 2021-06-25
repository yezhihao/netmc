package io.github.yezhihao.netmc.model;

import io.github.yezhihao.netmc.core.model.Message;
import io.github.yezhihao.netmc.session.Session;

public class MyMessage implements Message {

    private Session session;

    private MyHeader header;

    private String body;

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public MyHeader getHeader() {
        return header;
    }

    public void setHeader(MyHeader header) {
        this.header = header;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String getClientId() {
        return header.getClientId();
    }

    @Override
    public int getMessageId() {
        return header.getType();
    }

    @Override
    public String getMessageName() {
        return String.valueOf(header.getType());
    }

    @Override
    public int getSerialNo() {
        return header.getSerialNo();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MyMessage{");
        sb.append("session=").append(session);
        sb.append(", header=").append(header);
        sb.append(", body='").append(body).append('\'');
        sb.append('}');
        return sb.toString();
    }
}