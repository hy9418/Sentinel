package com.alibaba.csp.sentinel.support;

import java.util.Set;
import com.alibaba.csp.sentinel.slots.statistic.cache.CacheMap;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * @author Wolken Hu
 * @version $Id$
 * @since 2019/12/5 10:14
 */
public class RedisCacheMap<T, R> implements CacheMap<T, R> {

    private static final String PUT_IF_ABSENT = "if (redis.call(\"EXISTS\",KEYS[1]) == 1) then\n"
            + "return redis.call(\"GET\",KEYS[1])\nend\n"
            + "redis.call(\"SET\",KEYS[1],KEYS[2])\nreturn nil";
    private static final String SET_SUCCESS = "OK";
    private RedisCommands<Object, Object> sync;

    public RedisCacheMap(RedisCommands<Object, Object> commands) {
        this.sync = commands;
    }

    @Override
    public boolean containsKey(T key) {
        return sync.exists(key) == 1;
    }

    @Override
    public R get(T key) {
        return (R) sync.get(key);
    }

    @Override
    public R remove(T key) {
        sync.del(key);
        return null;
    }

    @Override
    public R put(T key, R value) {
        if (SET_SUCCESS.equals(sync.set(key, value))) {
            return value;
        }
        return null;
    }

    @Override
    public R putIfAbsent(T key, R value) {
        return sync.eval(PUT_IF_ABSENT, ScriptOutputType.VALUE, key, value);
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public void clear() {

    }

    @Override
    public Set<T> keySet(boolean ascending) {
        return null;
    }
}
