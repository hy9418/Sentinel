package com.alibaba.csp.sentinel.support;

import java.lang.reflect.Method;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisCommands;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * @author Wolken Hu
 * @version $Id$
 * @since 2019/12/6 11:29
 */
public class LettuceProxy {

    private static final String CAS_LUA = "if (redis.call(\"GET\",KEYS[1]) == KEYS[2]) then\n"
            + "redis.call(\"SET\",KEYS[1],KEYS[3])\nreturn true\nend\nreturn false";
    private static final String CAS = "compareAndSet";
    private static final String SET = "set";

    public static <T> T enhanceAtomicNumber(Class<T> atomicClass,
            final RedisCommands<Object, Object> sync, final Object key) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(atomicClass);
        if (sync != null) {
            enhancer.setCallback(new MethodInterceptor() {
                @Override
                public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy)
                        throws Throwable {
                    if (CAS.equals(method.getName())) {
                        return sync.eval(CAS_LUA, ScriptOutputType.BOOLEAN, key, args);
                    } else if (SET.equals(method.getName())) {
                        proxy.invokeSuper(obj, args);
                        sync.set(key, obj);
                    }
                    return proxy.invokeSuper(obj, args);
                }
            });
        }
        return (T) enhancer.create();
    }

}
