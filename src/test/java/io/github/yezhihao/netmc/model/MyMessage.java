package io.github.yezhihao.netmc.model;

import io.github.yezhihao.netmc.core.model.Message;
import io.github.yezhihao.netmc.session.Session;

public class MyMessage implements Message<MyHeader> {

    private Session session;

    private MyHeader header;

    private String body;

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    @Override
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
    public Object getMessageType() {
        return header.getType();
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
