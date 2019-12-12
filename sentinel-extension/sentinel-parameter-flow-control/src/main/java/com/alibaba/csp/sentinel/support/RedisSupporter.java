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
public class RedisSupporter {

    private static final String REDIS_COMMAND_PATH_PREFIX = "rap:gateway:";
    private static final String REDIS_SERVER = "rap.redis.server";
    private static final String REDIS_AUTH = "rap.redis.auth.password";
    private static final String AHAS_NAMESPACE = "ahas.namespace";
    private static final String PROJECT_NAME = "project.name";
    private static final String DEFAULT_PROJECT_NAME_GATEWAY = "rap-cloud-gateway";
    private static final String RAP_PROFILE = "rap.profile";
    private static final Map<String, CommandResource> COMMANDS_CACHE = new HashMap<>();
    private static final String REDIS_PROTOCOL = "redis://";
    private static AtomicBoolean init = new AtomicBoolean(false);
    private static RedissonClient redissonClient;
    private static String commandPrefix;

    public static CommandResource path(String path) {
        if (redissonClient == null) {
            if (!init.get()) {
                initialize();
            }
            if (redissonClient == null) {
                return null;
            }
        }
        String prefix = commandPrefix;
        if (path != null && !path.isEmpty()) {
            prefix += path;
        }
        if (COMMANDS_CACHE.get(prefix) == null) {
            synchronized (RedisSupporter.class) {
                if (COMMANDS_CACHE.get(prefix) == null) {
                    COMMANDS_CACHE.put(prefix, new CommandResource(prefix, redissonClient));
                }
            }
        }
        return COMMANDS_CACHE.get(prefix);
    }

    private static void prefix() {
        String prefix = REDIS_COMMAND_PATH_PREFIX;
        String namespace = System.getProperty(AHAS_NAMESPACE, System.getProperty(RAP_PROFILE));
        if (namespace != null && !namespace.isEmpty()) {
            prefix += namespace + ':';
        }
        String projectName = System.getProperty(PROJECT_NAME, DEFAULT_PROJECT_NAME_GATEWAY);
        if (!projectName.isEmpty()) {
            prefix += projectName + ':';
        }
        commandPrefix = prefix;
    }

    public static synchronized void initialize() {
        if (init.compareAndSet(false, true)) {
            String server = System.getProperty(REDIS_SERVER);
            if (server == null || "".equals(server) || !server.contains(":")) {
                throw new IllegalArgumentException("Redis server [rap.redis.server] not set. Format - <host>:<port>");
            }
            String password = System.getProperty(REDIS_AUTH);
            Config config = new Config();
            SingleServerConfig singleServerConfig = config.useSingleServer();
            singleServerConfig.setAddress(getOrAssembleProtocol(server));
            if (password != null) {
                singleServerConfig.setPassword(password);
            }
            redissonClient = Redisson.create(config);
            prefix();
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
