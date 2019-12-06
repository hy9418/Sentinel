package com.alibaba.csp.sentinel.support;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;

/**
 * @author Wolken Hu
 * @version $Id$
 * @since 2019/12/6 11:45
 */
public class LettuceSupporter {

    private static final String RAP_CLOUD_GATEWAY_CLUSTER_ENABLE =
            "rap.cloud.gateway.cluster.enable";
    private static final String REDIS_COMMAND_PREFIX = "rap-cloud-gateway:";
    private static final String REDIS_SERVER = "rap.redis.server";
    private static final String REDIS_AUTH = "rap.redis.auth.password";
    private static final Map<String, CommandWrapper> COMMANDS_CACHE = new HashMap<>();
    private static AtomicBoolean init = new AtomicBoolean(false);
    private static RedisClient redisClient;

    public static void initialize() {
        if (init.compareAndSet(false, true)) {
            if (!Boolean.valueOf(System.getProperty(RAP_CLOUD_GATEWAY_CLUSTER_ENABLE, "true"))) {
                return;
            }
            String server = System.getProperty(REDIS_SERVER);
            if (server == null || "".equals(server) || !server.contains(":")) {
                throw new IllegalArgumentException(
                        "Redis server [rap.redis.server] not set. Format - <host>:<port>");
            }
            String password = System.getProperty(REDIS_AUTH);
            String[] uri = server.split(":");
            RedisURI redisURI = RedisURI.create(uri[0], Integer.valueOf(uri[1]));
            if (password != null) {
                redisURI.setPassword(password);
            }
            redisClient = RedisClient.create(redisURI);
        }
    }

    public static CommandWrapper sync(String path) {
        if (!init.get()) {
            initialize();
        }
        if (redisClient == null) {
            return null;
        }
        String prefix = REDIS_COMMAND_PREFIX;
        if (path != null && !"".equals(path)) {
            prefix += path;
        }
        if (COMMANDS_CACHE.get(prefix) == null) {
            synchronized (LettuceSupporter.class) {
                if (COMMANDS_CACHE.get(prefix) == null) {
                    RedisCommands<Object, Object> sync =
                            redisClient.connect(new SerializedObjectCodec(prefix)).sync();
                    COMMANDS_CACHE.put(prefix, new CommandWrapper(sync, prefix));
                }
            }
        }
        return COMMANDS_CACHE.get(prefix);
    }

}
