package io.github.yezhihao.netmc.endpoint;

import io.github.yezhihao.netmc.core.annotation.Endpoint;
import io.github.yezhihao.netmc.core.annotation.Mapping;
import io.github.yezhihao.netmc.model.MyHeader;
import io.github.yezhihao.netmc.model.MyMessage;
import io.github.yezhihao.netmc.session.Session;

@Endpoint
public class MyEndpoint {

    @Mapping(types = 1, desc = "注册")
    public MyMessage register(MyMessage request, Session session) {
        session.register(request);
        return new MyMessage(new MyHeader(2, "123", 2), "ack");
    }
}
