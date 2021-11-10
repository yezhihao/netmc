package io.github.yezhihao.netmc.core.handler;

import io.github.yezhihao.netmc.core.model.Message;
import io.github.yezhihao.netmc.session.Session;

import java.lang.reflect.Method;

/**
 * 同步处理
 * @author yezhihao
 * home https://gitee.com/yezhihao/jt808-server
 */
public class SimpleHandler extends Handler {

    public SimpleHandler(Object actionClass, Method actionMethod, String desc) {
        super(actionClass, actionMethod, desc);
    }

    public <T extends Message> T invoke(T request, Session session) throws Exception {
        return super.invoke(request, session);
    }
}