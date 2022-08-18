package io.github.yezhihao.netmc.core.handler;

import io.github.yezhihao.netmc.core.model.Message;
import io.github.yezhihao.netmc.session.Session;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public abstract class Handler {

    public static final int MESSAGE = 0;
    public static final int SESSION = 1;

    public final Object targetObject;
    public final Method targetMethod;
    public final int[] parameterTypes;
    public final boolean returnVoid;
    public final boolean async;
    public final String desc;

    public Handler(Object targetObject, Method targetMethod, String desc) {
        this(targetObject, targetMethod, desc, false);
    }

    public Handler(Object targetObject, Method targetMethod, String desc, boolean async) {
        this.targetObject = targetObject;
        this.targetMethod = targetMethod;
        this.returnVoid = targetMethod.getReturnType().isAssignableFrom(Void.TYPE);
        this.async = async;
        if (desc == null || desc.isEmpty())
            desc = targetMethod.getName();
        this.desc = desc;

        Type[] types = targetMethod.getGenericParameterTypes();
        int[] parameterTypes = new int[types.length];
        try {
            for (int i = 0; i < types.length; i++) {
                Type type = types[i];
                Class<?> clazz;
                if (type instanceof ParameterizedType)
                    clazz = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
                else
                    clazz = (Class<?>) type;

                if (Message.class.isAssignableFrom(clazz))
                    parameterTypes[i] = MESSAGE;
                else if (Session.class.isAssignableFrom(clazz))
                    parameterTypes[i] = SESSION;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.parameterTypes = parameterTypes;
    }

    public <T extends Message> T invoke(T request, Session session) throws Exception {
        Object[] args = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            int type = parameterTypes[i];
            switch (type) {
                case Handler.MESSAGE:
                    args[i] = request;
                    break;
                case Handler.SESSION:
                    args[i] = session;
                    break;
            }
        }
        return (T) targetMethod.invoke(targetObject, args);
    }

    @Override
    public String toString() {
        return desc;
    }
}