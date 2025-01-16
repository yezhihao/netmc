package io.github.yezhihao.netmc.endpoint;

import io.github.yezhihao.netmc.core.HandlerInterceptor;
import io.github.yezhihao.netmc.model.MyHeader;
import io.github.yezhihao.netmc.model.MyMessage;
import io.github.yezhihao.netmc.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyHandlerInterceptor implements HandlerInterceptor<MyMessage> {

    private static final Logger log = LoggerFactory.getLogger(MyHandlerInterceptor.class.getSimpleName());

    /** 未找到对应的Handle */
    @Override
    public MyMessage notSupported(MyMessage request, Session session) {
        log.warn(">>>>>>>>>>未识别的消息{},{}", session, request);

        MyHeader header = request.getHeader();
        MyMessage response = new MyMessage();
        response.setHeader(new MyHeader(400, header.getClientId(), session.nextSerialNo()));
        response.setBody("success");

        log.info("<<<<<<<<<<未识别的消息{},{}", session, response);
        return response;
    }


    /** 调用之后，返回值为void的 */
    @Override
    public MyMessage successful(MyMessage request, Session session) {
        log.info(">>>>>>>>>>消息请求成功{},{}", session, request);

        MyHeader header = request.getHeader();
        MyMessage response = new MyMessage();
        response.setHeader(new MyHeader(200, header.getClientId(), session.nextSerialNo()));
        response.setBody("success");

        log.info("<<<<<<<<<<通用应答消息{},{}", session, response);
        return response;
    }

    /** 调用之后抛出异常的 */
    @Override
    public MyMessage exceptional(MyMessage request, Session session, Throwable ex) {
        log.warn(">>>>>>>>>>消息处理异常{},{}", session, request);

        MyHeader header = request.getHeader();
        MyMessage response = new MyMessage();
        response.setHeader(new MyHeader(500, header.getClientId(), session.nextSerialNo()));
        response.setBody("error");

        log.info("<<<<<<<<<<异常处理应答{},{}", session, response);
        return response;
    }

    /** 调用之前 */
    @Override
    public boolean beforeHandle(MyMessage request, Session session) {
        request.setSession(session);
        return true;
    }

    /** 调用之后 */
    @Override
    public void afterHandle(MyMessage request, MyMessage response, Session session) {
        log.info(">>>>>>>>>>消息请求成功{},{}", session, request);
        log.info("<<<<<<<<<<应答消息{},{}", session, response);
    }
}