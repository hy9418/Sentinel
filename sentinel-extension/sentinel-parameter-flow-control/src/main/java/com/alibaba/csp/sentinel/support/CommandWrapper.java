package com.alibaba.csp.sentinel.support;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * @author Wolken Hu
 * @version $Id$
 * @since 2019/12/6 15:46
 */
public class CommandWrapper {

    private final RedisCommands<Object, Object> redisCommands;
    private final String keysPattern;

    public CommandWrapper(RedisCommands<Object, Object> redisCommands, String prefix) {
        this.redisCommands = redisCommands;
        this.keysPattern = prefix + "*";
    }

    public RedisCommands<Object, Object> getRedisCommands() {
        return redisCommands;
    }

    public void clear() {
        List<Object> keys = redisCommands.keys(keysPattern);
        if (keys != null && !keys.isEmpty()) {
            redisCommands.del(keys);
        }
    }

    public Set<Object> keySet() {
        return new HashSet<>(redisCommands.keys(keysPattern));
    }
}
