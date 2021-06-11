package io.github.yezhihao.netmc.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.channel.Channel;
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

    private Map<Object, Session> sessionMap;

    private Cache<Object, Integer> versionCache;

    private ChannelFutureListener remover;

    private SessionListener sessionListener;

    private Class<? extends Enum> sessionKeyClass;

    public SessionManager() {

    }

    public SessionManager(Class<? extends Enum> sessionKeyClass) {
        this(sessionKeyClass, null);
    }

    public SessionManager(SessionListener sessionListener) {
        this(null, sessionListener);
    }

    public SessionManager(Class<? extends Enum> sessionKeyClass, SessionListener sessionListener) {
        this.sessionMap = new ConcurrentHashMap<>();
        this.versionCache = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();
        this.sessionKeyClass = sessionKeyClass;
        this.remover = future -> {
            Session session = future.channel().attr(Session.KEY).get();
            if (session != null) {
                sessionMap.remove(session.getClientId(), session);
            }
        };
        this.sessionListener = sessionListener;
    }

    public Session newSession(Channel channel) {
        Session session = new Session(sessionKeyClass, channel, this);
        callSessionCreatedListener(session);
        return session;
    }

    protected void callSessionDestroyedListener(Session session) {
        if (sessionListener != null)
            sessionListener.sessionDestroyed(session);
    }

    protected void callSessionCreatedListener(Session session) {
        if (sessionListener != null)
            sessionListener.sessionCreated(session);
    }

    public Session get(Object clientId) {
        return sessionMap.get(clientId);
    }

    public Collection<Session> all() {
        return sessionMap.values();
    }

    protected void put(Object clientId, Session newSession) {
        Session oldSession = sessionMap.put(clientId, newSession);
        if (!newSession.equals(oldSession)) {
            newSession.channel.closeFuture().addListener(remover);
        }
    }

    public void putVersion(Object clientId, int version) {
        versionCache.put(clientId, version);
    }

    public Integer getVersion(Object clientId) {
        return versionCache.getIfPresent(clientId);
    }
}