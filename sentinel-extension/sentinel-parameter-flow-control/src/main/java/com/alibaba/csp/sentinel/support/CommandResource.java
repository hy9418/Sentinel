package com.alibaba.csp.sentinel.support;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.codec.JsonJacksonCodec;

/**
 * @author Wolken Hu
 * @version $Id$
 * @since 2019/12/6 15:46
 */
public class CommandResource {

    private static final String PUT_IF_ABSENT =
            "if (redis.call('SETNX',KEYS[1],ARGV[1]) == 1) then\nreturn nil\nend\nreturn redis"
                    + ".call('GET',KEYS[1])";
    private static final Codec JSON_JACKSON_CODEC = new JsonJacksonCodec();
    private final String prefix;
    private final String pattern;
    private RedissonClient redissonClient;

    public CommandResource(String prefix, RedissonClient redissonClient) {
        this.prefix = prefix;
        this.pattern = prefix + "*";
        this.redissonClient = redissonClient;
    }

    public void clear() {
        redissonClient.getKeys().deleteByPattern(pattern);
    }

    public Set<Object> keySet() {
        HashSet<Object> result = new HashSet<>();
        Iterable<String> keysByPattern = redissonClient.getKeys().getKeysByPattern(pattern);
        keysByPattern.forEach(s -> result.add(s));
        return result;
    }

    public <T> RAtomicLong getOrNewAtomicLong(T key) {
        return redissonClient.getAtomicLong(prefix + key.toString());
    }

    public <T> Long exists(T... keys) {
        String[] name = toStringArray(keys);
        return redissonClient.getKeys().countExists(name);
    }

    public <T, R> boolean set(T key, R value) {
        RBucket<Object> bucket =
                redissonClient.getBucket(prefix + key.toString(), JSON_JACKSON_CODEC);
        bucket.set(value);
        return true;
    }

    public <T> void del(T... keys) {
        String[] name = toStringArray(keys);
        redissonClient.getKeys().delete(name);
    }

    public <T, R> R putIfAbsent(T key, R value) {
        return redissonClient.getScript()
                .eval(RScript.Mode.READ_WRITE, JSON_JACKSON_CODEC, PUT_IF_ABSENT,
                        RScript.ReturnType.VALUE,
                        Collections.singletonList(prefix + key.toString()), value);
    }

    public long size() {
        return redissonClient.getKeys().findKeysByPattern(pattern).size();
    }

    private <T> String[] toStringArray(T... keys) {
        String[] name = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            name[i] = prefix + keys[i].toString();
        }
        return name;
    }

    public <T, R> boolean compareAndSet(T key, R expect, R newValue) {
        RBucket<Object> bucket = redissonClient.getBucket(prefix + key);
        return bucket.compareAndSet(expect, newValue);
    }

    public <T> long incrementAndGet(T key) {
        return redissonClient.getAtomicLong(prefix + key).incrementAndGet();
    }
}
