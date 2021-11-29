package io.github.yezhihao.netmc.session;

public enum MessageState {
    SUCCESS("成功"),
    TIME_OUT("消息发送超时"),
    SEND_FAILED("消息发送失败"),
    NOT_RESPONDING("客户端未响应"),
    SUBSCRIPTION_FAILED("消息订阅失败");

    final String message;

    public String getMessage() {
        return message;
    }

    MessageState(String message) {
        this.message = message;
    }
}