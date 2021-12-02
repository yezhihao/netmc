package io.github.yezhihao.netmc.session;

import io.github.yezhihao.netmc.core.model.Message;
import io.github.yezhihao.netmc.core.model.Response;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.AttributeKey;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yezhihao
 * home https://gitee.com/yezhihao/jt808-server
 */
public class Session {

    public static final AttributeKey<Session> KEY = AttributeKey.newInstance(Session.class.getName());

    protected final Channel channel;
    private final SessionManager sessionManager;
    private final SessionListener sessionListener;

    private final long creationTime;
    private long lastAccessedTime;
    private final Map<Object, Object> attributes;

    private String sessionId;
    private String clientId;
    private final AtomicInteger serialNo = new AtomicInteger(0);


    private Session(Channel channel, SessionManager sessionManager, SessionListener sessionListener) {
        this.channel = channel;
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = creationTime;
        this.sessionManager = sessionManager;
        this.sessionListener = sessionListener;

        if (sessionManager != null && sessionManager.getSessionKeyClass() != null)
            this.attributes = new EnumMap(sessionManager.getSessionKeyClass());
        else
            this.attributes = new TreeMap<>();
    }

    public static Session newInstance(Channel channel,
                                      SessionManager sessionManager,
                                      SessionListener sessionListener) {
        Session session = new Session(channel, sessionManager, sessionListener);
        session.callSessionCreatedListener();
        return session;
    }

    /**
     * 注册到SessionManager
     */
    public void register(Message message) {
        register(message.getClientId(), message);
    }

    public void register(String sessionId, Message message) {
        if (sessionId == null)
            throw new NullPointerException("sessionId not null");
        this.sessionId = sessionId;
        this.clientId = message.getClientId();
        if (sessionManager != null)
            sessionManager.add(this);
        if (sessionListener != null)
            sessionListener.sessionRegistered(this);
    }

    public boolean isRegistered() {
        return sessionId != null;
    }

    public String getId() {
        return sessionId;
    }

    public String getClientId() {
        return clientId;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public long access() {
        lastAccessedTime = System.currentTimeMillis();
        return lastAccessedTime;
    }

    public Collection<Object> getAttributeNames() {
        return attributes.keySet();
    }

    public Map<Object, Object> getAttributes() {
        return attributes;
    }

    public Object getAttribute(Object name) {
        return attributes.get(name);
    }

    public void setAttribute(Object name, Object value) {
        attributes.put(name, value);
    }

    public Object removeAttribute(Object name) {
        return attributes.remove(name);
    }

    public Object getOfflineCache(String clientId) {
        if (sessionManager != null)
            return sessionManager.getOfflineCache(clientId);
        return null;
    }

    public void setOfflineCache(String clientId, Object value) {
        if (sessionManager != null)
            sessionManager.setOfflineCache(clientId, value);
    }

    private void callSessionDestroyedListener() {
        if (sessionListener != null)
            sessionListener.sessionDestroyed(this);
    }

    private void callSessionCreatedListener() {
        if (sessionListener != null)
            sessionListener.sessionCreated(this);
    }

    public int nextSerialNo() {
        int current;
        int next;
        do {
            current = serialNo.get();
            next = current > 0xffff ? 0 : current;
        } while (!serialNo.compareAndSet(current, next + 1));
        return next;
    }

    public void invalidate() {
        channel.close();
        callSessionDestroyedListener();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(66);
        sb.append("{sid=").append(sessionId);
        sb.append(",cid=").append(clientId);
        sb.append(",ip=").append(channel.remoteAddress());
        sb.append('}');
        return sb.toString();
    }

    private final Map<String, MonoSink<Message>> topicSubscribers = new HashMap<>();

    private static final Mono Rejected = Mono.error(new RejectedExecutionException("客户端暂未响应，请勿重复发送"));

    /**
     * 异步发送通知类消息
     * 同步发送 mono.block()
     * 订阅回调 mono.doOnSuccess({处理成功}).doOnError({处理异常}).subscribe()开始订阅
     */
    public Mono<Void> notify(Object message) {
        ChannelFuture channelFuture = channel.writeAndFlush(message);
        Mono<Void> mono = Mono.create(sink -> channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                sink.success();
            } else {
                sink.error(future.cause());
            }
        }));
        return mono;
    }

    /**
     * 异步发送消息，接收响应
     * 同步接收 mono.block()
     * 订阅回调 mono.doOnSuccess({处理成功}).doOnError({处理异常}).subscribe()开始订阅
     */
    public <T> Mono<T> request(Message request, Class<T> responseClass) {
        String key = requestKey(request, responseClass);
        Mono<Message> receive = this.subscribe(key);
        if (receive == null) {
            return Rejected;
        }

        ChannelFuture channelFuture = channel.writeAndFlush(request);
        Mono<Message> mono = Mono.create(sink -> channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                sink.success(future);
            } else {
                sink.error(future.cause());
            }
        })).then(receive).doFinally(signal -> unsubscribe(key));
        return (Mono<T>) mono;
    }

    /**
     * 消息响应
     */
    public boolean response(Message message) {
        MonoSink<Message> sink = topicSubscribers.get(responseKey(message));
        if (sink != null) {
            sink.success(message);
            return true;
        }
        return false;
    }

    private Mono<Message> subscribe(String key) {
        synchronized (topicSubscribers) {
            if (!topicSubscribers.containsKey(key)) return Mono.create(sink -> topicSubscribers.put(key, sink));
        }
        return null;
    }

    private void unsubscribe(String key) {
        topicSubscribers.remove(key);
    }

    private static String requestKey(Message request, Class responseClass) {
        String className = responseClass.getName();
        if (Response.class.isAssignableFrom(responseClass)) {
            int serialNo = request.getSerialNo();
            return new StringBuilder(34).append(className).append('.').append(serialNo).toString();
        }
        return className;
    }

    private static String responseKey(Message response) {
        String className = response.getClass().getName();
        if (response instanceof Response) {
            int serialNo = ((Response) response).getResponseSerialNo();
            return new StringBuilder(34).append(className).append('.').append(serialNo).toString();
        }
        return className;
    }
}