package io.github.yezhihao.netmc.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.channel.ChannelFutureListener;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author yezhihao
 * home https://gitee.com/yezhihao/jt808-server
 */
public class SessionManager {

    private Map<String, Session> sessionMap;

    private Cache<String, Object> offlineCache;

    private ChannelFutureListener remover;

    private Class<? extends Enum> sessionKeyClass;

    public SessionManager() {
        this(null);
    }

    public SessionManager(Class<? extends Enum> sessionKeyClass) {
        this.sessionMap = new ConcurrentHashMap<>();
        this.offlineCache = Caffeine.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build();
        this.sessionKeyClass = sessionKeyClass;
        this.remover = future -> {
            Session session = future.channel().attr(Session.KEY).get();
            if (session != null) {
                sessionMap.remove(session.getId(), session);
            }
        };
    }

    public Session get(String sessionId) {
        return sessionMap.get(sessionId);
    }

    public Collection<Session> all() {
        return sessionMap.values();
    }

    protected void add(Session newSession) {
        Session oldSession = sessionMap.put(newSession.getId(), newSession);
        if (!newSession.equals(oldSession)) {
            newSession.channel.closeFuture().addListener(remover);
        }
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