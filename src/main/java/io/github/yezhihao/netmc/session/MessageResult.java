package io.github.yezhihao.netmc.session;

/**
 * @author yezhihao
 * home https://gitee.com/yezhihao/jt808-server
 */
public class MessageResult<T> {

    private T value;
    private MessageState state;
    private Throwable throwable;

    public MessageResult(T value) {
        this.value = value;
    }

    public MessageResult(MessageState state) {
        this.state = state;
    }

    public MessageResult(MessageState state, Throwable throwable) {
        this.state = state;
        this.throwable = throwable;
    }

    public T value() {
        return value;
    }

    public MessageState state() {
        return state;
    }

    public Throwable cause() {
        return throwable;
    }

    public boolean isSuccess() {
        return MessageState.SUCCESS.equals(state);
    }
}