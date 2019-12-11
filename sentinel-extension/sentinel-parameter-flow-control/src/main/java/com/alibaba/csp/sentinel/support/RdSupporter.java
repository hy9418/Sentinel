package com.alibaba.csp.sentinel.support;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.misc.URIBuilder;

/**
 * @author Wolken Hu
 * @version $Id$
 * @since 2019/12/6 11:45
 */
public class RdSupporter {

    private static final String REDIS_COMMAND_PREFIX = "rap-cloud-gateway:";
    private static final String REDIS_SERVER = "rap.redis.server";
    private static final String REDIS_AUTH = "rap.redis.auth.password";
    private static final Map<String, CommandResource> COMMANDS_CACHE = new HashMap<>();
    private static final String REDIS_PROTOCOL = "redis://";
    private static AtomicBoolean init = new AtomicBoolean(false);
    private static RedissonClient redissonClient;

    public static CommandResource path(String path) {
        if (!init.get()) {
            initialize();
        }
        if (redissonClient == null) {
            return null;
        }
        String prefix = REDIS_COMMAND_PREFIX;
        if (path != null && !"".equals(path)) {
            prefix += path;
        }
        if (COMMANDS_CACHE.get(prefix) == null) {
            synchronized (RdSupporter.class) {
                if (COMMANDS_CACHE.get(prefix) == null) {
                    COMMANDS_CACHE.put(prefix, new CommandResource(prefix, redissonClient));
                }
            }
        }
        return COMMANDS_CACHE.get(prefix);
    }

    public static synchronized void initialize() {
        if (init.compareAndSet(false, true)) {
            String server = System.getProperty(REDIS_SERVER);
            if (server == null || "".equals(server) || !server.contains(":")) {
                throw new IllegalArgumentException(
                        "Redis server [rap.redis.server] not set. Format - <host>:<port>");
            }
            String password = System.getProperty(REDIS_AUTH);
            Config config = new Config();
            SingleServerConfig singleServerConfig = config.useSingleServer();
            singleServerConfig.setAddress(getOrAssembleProtocol(server));
            if (password != null) {
                singleServerConfig.setPassword(password);
            }
            redissonClient = Redisson.create(config);
        }
    }

    private static String getOrAssembleProtocol(String raw) {
        URI uri;
        boolean haveProtocol = raw.startsWith(REDIS_PROTOCOL);
        String assembled = haveProtocol ? raw : REDIS_PROTOCOL + raw;
        String result = raw;
        if (compatibleRedissonVersion()) {
            uri = URIBuilder.create(assembled);
            if (uri.getHost() != null && uri.getPort() < 0xffff && uri.getPort() > 0) {
                result = assembled;
            }
        } else {
            result = excludeProtocol(REDIS_PROTOCOL, raw);
        }
        return result;
    }

    private static boolean compatibleRedissonVersion() {
        boolean b;
        try {
            Class.forName("org.redisson.misc.URIBuilder");
            b = true;
        } catch (ClassNotFoundException e) {
            b = false;
        }
        return b;
    }

    private static String excludeProtocol(String protocol, String raw) {
        return raw.replace(protocol, "");
    }
}
