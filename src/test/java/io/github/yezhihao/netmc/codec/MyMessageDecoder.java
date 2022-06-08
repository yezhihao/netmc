package io.github.yezhihao.netmc.codec;

import io.github.yezhihao.netmc.model.MyHeader;
import io.github.yezhihao.netmc.model.MyMessage;
import io.github.yezhihao.netmc.session.Session;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class MyMessageDecoder implements MessageDecoder {

    @Override
    public MyMessage decode(ByteBuf buf, Session session) {
        String msgStr = buf.readCharSequence(buf.readableBytes(), StandardCharsets.UTF_8).toString();
        String[] allStr = msgStr.split(";");
        String[] headStr = allStr[0].split(",");
        String bodyStr = allStr[1];

        MyHeader header = new MyHeader();
        header.setClientId(headStr[0]);
        header.setType(Integer.parseInt(headStr[1]));
        header.setSerialNo(Integer.parseInt(headStr[2]));

        MyMessage message = new MyMessage();
        message.setHeader(header);
        message.setBody(bodyStr);
        return message;
    }
}