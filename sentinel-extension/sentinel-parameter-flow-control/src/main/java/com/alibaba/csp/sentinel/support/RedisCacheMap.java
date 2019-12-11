package com.alibaba.csp.sentinel.support;

import java.util.Set;
import com.alibaba.csp.sentinel.slots.statistic.cache.CacheMap;
import com.alibaba.csp.sentinel.util.AssertUtil;
import org.redisson.api.RAtomicLong;

/**
 * @author Wolken Hu
 * @version $Id$
 * @since 2019/12/5 10:14
 */
@SuppressWarnings("unchecked")
public class RedisCacheMap<T> implements CacheMap<T, RAtomicLong> {

    private CommandResource commandResource;

    public RedisCacheMap(CommandResource commands) {
        AssertUtil.notNull(commands, "Redis setting error");
        this.commandResource = commands;
    }

    @Override
    public boolean containsKey(T key) {
        return commandResource.exists(key) == 1;
    }

    @Override
    public RAtomicLong get(T key) {
        return commandResource.getOrNewAtomicLong(key.toString());
    }

    @Override
    public RAtomicLong remove(T key) {
        commandResource.del(key);
        //useless return
        return null;
    }

    @Override
    public RAtomicLong put(T key, RAtomicLong value) {
        if (value != null) {
            commandResource.set(key, value);
        }
        return value;
    }

    @Override
    public RAtomicLong putIfAbsent(T key, RAtomicLong value) {
        if (value != null) {
            return commandResource.putIfAbsent(key.toString(), value);
        }
        return null;
    }

    @Override
    public long size() {
        return commandResource.size();
    }

    @Override
    public void clear() {
        commandResource.clear();
    }

    @Override
    public Set<T> keySet(boolean ascending) {
        return (Set<T>) commandResource.keySet();
    }
}
