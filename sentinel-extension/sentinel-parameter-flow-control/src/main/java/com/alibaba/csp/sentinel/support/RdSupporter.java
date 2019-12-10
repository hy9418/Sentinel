package com.alibaba.csp.sentinel.support;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;

/**
 * @author Wolken Hu
 * @version $Id$
 * @since 2019/12/6 11:45
 */
public class RdSupporter {

    private static final String RAP_CLOUD_GATEWAY_CLUSTER_ENABLE =
            "rap.cloud.gateway.cluster.enable";
    private static final String REDIS_COMMAND_PREFIX = "rap-cloud-gateway:";
    private static final String REDIS_SERVER = "rap.redis.server";
    private static final String REDIS_AUTH = "rap.redis.auth.password";
    private static final Map<String, CommandResource> COMMANDS_CACHE = new HashMap<>();
    private static AtomicBoolean init = new AtomicBoolean(false);
    private static RedissonClient redissonClient;

    public static synchronized void initialize() {
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
            Config config = new Config();
            //TODO 兼容协议
            SingleServerConfig singleServerConfig = config.useSingleServer();
            singleServerConfig.setAddress(server);
            if (password != null) {
                singleServerConfig.setPassword(password);
            }
            redissonClient = Redisson.create(config);
        }
    }

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
}
