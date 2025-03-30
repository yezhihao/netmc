package io.github.yezhihao.netmc.session;

import io.github.yezhihao.netmc.core.model.Message;
import io.github.yezhihao.netmc.core.model.Response;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public class Session {

    private static final Logger log = LoggerFactory.getLogger(Session.class);

    private final boolean udp;
    private final Function<Session, Boolean> remover;
    protected final Channel channel;
    private final SessionManager sessionManager;
    private final InetSocketAddress remoteAddress;
    private final String remoteAddressStr;

    private final long creationTime;
    private long lastAccessedTime;
    private final Map<Object, Object> attributes;

    private String sessionId;
    private String clientId;
    private final AtomicInteger serialNo = new AtomicInteger(0);
    private BiConsumer<Session, Message> requestInterceptor = (session, message) -> {
    };
    private BiConsumer<Session, Message> responseInterceptor = (session, message) -> {
    };

    public Session(SessionManager sessionManager,
                   Channel channel,
                   InetSocketAddress remoteAddress,
                   Function<Session, Boolean> remover,
                   boolean udp) {
        this.channel = channel;
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = creationTime;
        this.sessionManager = sessionManager;
        this.remoteAddress = remoteAddress;
        this.remoteAddressStr = remoteAddress.toString();
        this.remover = remover;
        this.udp = udp;

        if (sessionManager != null && sessionManager.getSessionKeyClass() != null)
            this.attributes = new EnumMap(sessionManager.getSessionKeyClass());
        else
            this.attributes = new TreeMap<>();
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
        log.info("<<<<< Registered{}", this);
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

    public <T> T getAttribute(Object name) {
        return (T) attributes.get(name);
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

    public InetSocketAddress remoteAddress() {
        return remoteAddress;
    }

    public void requestInterceptor(BiConsumer<Session, Message> requestInterceptor) {
        if (requestInterceptor != null)
            this.requestInterceptor = requestInterceptor;
    }

    public void responseInterceptor(BiConsumer<Session, Message> responseInterceptor) {
        if (responseInterceptor != null)
            this.responseInterceptor = responseInterceptor;
    }

    private static final IntUnaryOperator UNARY_OPERATOR = prev -> prev >= 0xFFFF ? 0 : prev + 1;

    public int nextSerialNo() {
        return serialNo.getAndUpdate(UNARY_OPERATOR);
    }

    public void invalidate() {
        if (isRegistered() && sessionManager != null)
            sessionManager.remove(this);
        remover.apply(this);
    }

    public String getRemoteAddressStr() {
        return remoteAddressStr;
    }

    public boolean isUdp() {
        return udp;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(50);
        sb.append(remoteAddressStr);
        sb.append('/').append(sessionId);
        if (!Objects.equals(sessionId, clientId))
            sb.append('/').append(clientId);
        return sb.toString();
    }

    /**
     * 异步发送通知类消息
     * 同步发送 mono.block()
     * 订阅回调 mono.doOnSuccess({处理成功}).doOnError({处理异常}).subscribe()开始订阅
     */
    public Mono<Void> notify(Message message) {
        requestInterceptor.accept(this, message);
        Packet packet = Packet.of(this, message);
        return Mono.create(sink -> channel.writeAndFlush(packet).addListener(future -> {
            if (future.isSuccess()) {
                sink.success();
            } else {
                sink.error(future.cause());
            }
        }));
    }

    public Mono<Void> notify(ByteBuf message) {
        Packet packet = Packet.of(this, message);
        return Mono.create(sink -> channel.writeAndFlush(packet).addListener(future -> {
            if (future.isSuccess()) {
                sink.success();
            } else {
                sink.error(future.cause());
            }
        }));
    }

    private final Map<String, MonoSink> subscribers = new ConcurrentHashMap<>();

    /**
     * 异步发送消息，接收响应（默认超时时间30秒）
     * 同步接收 mono.block()
     * 订阅回调 mono.doOnSuccess({处理成功}).doOnError({处理异常}).subscribe()开始订阅
     */
    public <T> Mono<T> request(Message request, Class<T> responseClass) {
        String key = requestKey(request, responseClass);
        return Mono.<T>create(sink -> {
            if (subscribers.putIfAbsent(key, sink) != null) {
                sink.error(new IllegalStateException("等待应答中，请勿重复发送"));
            } else {
                sink.onDispose(() -> subscribers.remove(key, sink));
                requestInterceptor.accept(this, request);
                Packet packet = Packet.of(this, request);

                channel.writeAndFlush(packet).addListener(future -> {
                    if (!future.isSuccess()) {
                        sink.error(future.cause());
                    }
                });
            }
        }).timeout(Duration.ofSeconds(30L));
    }

    /**
     * 消息响应
     */
    public boolean response(Message message) {
        responseInterceptor.accept(this, message);
        MonoSink<Message> sink = subscribers.get(responseKey(message));
        if (sink != null) {
            sink.success(message);
            return true;
        }
        return false;
    }

    private static String requestKey(Message request, Class<?> responseClass) {
        String className = responseClass.getName();
        if (Response.class.isAssignableFrom(responseClass)) {
            return className + '.' + request.getSerialNo();
        }
        return className;
    }

    private static String responseKey(Object response) {
        String className = response.getClass().getName();
        if (response instanceof Response resp) {
            return className + '.' + resp.getResponseSerialNo();
        }
        return className;
    }
}