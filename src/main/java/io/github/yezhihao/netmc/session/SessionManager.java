package io.github.yezhihao.netmc.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public class SessionManager {

    private final Map<String, Session> sessionMap;

    private final Cache<String, Object> offlineCache;

    private final SessionListener sessionListener;

    private final Class<? extends Enum> sessionKeyClass;

    public SessionManager() {
        this(null, null);
    }

    public SessionManager(SessionListener sessionListener) {
        this(null, sessionListener);
    }

    public SessionManager(Class<? extends Enum> sessionKeyClass, SessionListener sessionListener) {
        this.sessionMap = new ConcurrentHashMap<>();
        this.offlineCache = Caffeine.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build();
        this.sessionKeyClass = sessionKeyClass;
        this.sessionListener = sessionListener;
    }

    public Session get(String sessionId) {
        return sessionMap.get(sessionId);
    }

    public Collection<Session> all() {
        return sessionMap.values();
    }

    public Session newInstance(Channel channel) {
        InetSocketAddress sender = (InetSocketAddress) channel.remoteAddress();
        Session session = new Session(this, channel, sender, s -> {
            channel.close();
            return true;
        }, false);
        if (sessionListener != null)
            sessionListener.sessionCreated(session);
        return session;
    }

    public Session newInstance(Channel channel, InetSocketAddress sender, Function<Session, Boolean> remover) {
        Session session = new Session(this, channel, sender, remover, true);
        if (sessionListener != null)
            sessionListener.sessionCreated(session);
        return session;
    }

    protected void remove(Session session) {
        boolean remove = sessionMap.remove(session.getId(), session);
        if (remove && sessionListener != null)
            sessionListener.sessionDestroyed(session);
    }

    protected void add(Session newSession) {
        Session oldSession = sessionMap.put(newSession.getId(), newSession);
        if (sessionListener != null)
            sessionListener.sessionRegistered(newSession);
    }

    public void setOfflineCache(String clientId, Object value) {
        offlineCache.put(clientId, value);
    }

    public Object getOfflineCache(String clientId) {
        return offlineCache.getIfPresent(clientId);
    }

    public Class<? extends Enum> getSessionKeyClass() {
        return sessionKeyClass;
    }
}