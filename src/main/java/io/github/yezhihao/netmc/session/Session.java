package io.github.yezhihao.netmc.session;

import io.github.yezhihao.netmc.core.model.Message;
import io.github.yezhihao.netmc.core.model.Response;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yezhihao
 * home https://gitee.com/yezhihao/jt808-server
 */
public class Session {

    private static final Logger log = LoggerFactory.getLogger(Session.class.getSimpleName());

    public static final AttributeKey<Session> KEY = AttributeKey.newInstance(Session.class.getName());

    protected final Channel channel;
    private final SessionManager sessionManager;
    private final SessionListener sessionListener;

    private final long creationTime;
    private volatile long lastAccessedTime;
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

    public int nextSerialNo() {
        int current;
        int next;
        do {
            current = serialNo.get();
            next = current > 0xffff ? 0 : current;
        } while (!serialNo.compareAndSet(current, next + 1));
        return next;
    }

    public boolean isRegistered() {
        return sessionId != null;
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

    public void invalidate() {
        channel.close();
        callSessionDestroyedListener();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(66);
        sb.append("[ip=").append(channel.remoteAddress());
        sb.append(", sid=").append(sessionId);
        sb.append(", cid=").append(clientId);
        sb.append(']');
        return sb.toString();
    }

    private transient Map<String, SynchronousQueue> topicSubscribers = new HashMap<>();

    private static final ChannelFutureListener ERROR_LOG_LISTENER = future -> {
        Throwable t = future.cause();
        if (t != null)
            log.error(">>>>>>>>>>消息下发失败", t);
    };

    /**
     * 发送通知类消息，不接收响应
     */
    public void notify(Object message) {
        log.info(">>>>>>>>>>消息通知{},{}", this, message);
        channel.writeAndFlush(message).addListener(ERROR_LOG_LISTENER);
    }

    /**
     * 发送同步消息，接收响应
     * 默认超时时间20秒
     */
    public <T> T request(Message request, Class<T> responseClass) {
        return request(request, responseClass, 20000);
    }

    public <T> T request(Message request, Class<T> responseClass, long timeout) {
        String key = requestKey(request, responseClass);
        SynchronousQueue syncQueue = this.subscribe(key);
        if (syncQueue == null) {
            log.info("==========请勿重复发送,{}", request);
        }

        T result = null;
        try {
            log.info(">>>>>>>>>>消息请求{},{}", this, request);
            ChannelFuture channelFuture = channel.writeAndFlush(request).addListener(ERROR_LOG_LISTENER);
            if (channelFuture.awaitUninterruptibly().isSuccess())
                result = (T) syncQueue.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            log.warn(">>>>>>>>>>等待响应超时" + this, e);
        } finally {
            this.unsubscribe(key);
        }
        return result;
    }

    /**
     * 消息响应
     */
    public boolean response(Message message) {
        SynchronousQueue queue = topicSubscribers.get(responseKey(message));
        if (queue != null)
            return queue.offer(message);
        return false;
    }

    private SynchronousQueue subscribe(String key) {
        SynchronousQueue queue = null;
        synchronized (this) {
            if (!topicSubscribers.containsKey(key))
                topicSubscribers.put(key, queue = new SynchronousQueue());
        }
        return queue;
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