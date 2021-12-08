package io.github.yezhihao.netmc.core;

import io.github.yezhihao.netmc.core.annotation.Endpoint;
import io.github.yezhihao.netmc.util.ClassUtils;

import java.util.List;

/**
 * @author yezhihao
 * https://gitee.com/yezhihao/jt808-server
 */
public class DefaultHandlerMapping extends AbstractHandlerMapping {

    public DefaultHandlerMapping(String endpointPackage) {
        List<Class> endpointClasses = ClassUtils.getClassList(endpointPackage, Endpoint.class);

        for (Class endpointClass : endpointClasses) {
            try {
                Object bean = endpointClass.getDeclaredConstructor((Class[]) null).newInstance((Object[]) null);
                super.registerHandlers(bean);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}