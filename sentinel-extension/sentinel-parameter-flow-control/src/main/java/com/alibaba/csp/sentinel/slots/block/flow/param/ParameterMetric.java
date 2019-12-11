/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.csp.sentinel.slots.block.flow.param;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.slots.statistic.cache.CacheMap;
import com.alibaba.csp.sentinel.support.CommandResource;
import com.alibaba.csp.sentinel.support.RdSupporter;
import com.alibaba.csp.sentinel.support.RedisCacheMap;
import org.redisson.api.RAtomicLong;

/**
 * Metrics for frequent ("hot spot") parameters.
 *
 * @author Eric Zhao
 * @since 0.2.0
 */
public class ParameterMetric {

    private static final String PATH_RULE_TIME_COUNTERS = "ruleTimeCounters:";
    private static final String PATH_RULE_TOKEN_COUNTER = "ruleTokenCounter:";
    private static final String PATH_THREAD_COUNT_MAP = "threadCountMap:";
    private static final int THREAD_COUNT_MAX_CAPACITY = 4000;
    private static final int BASE_PARAM_MAX_CAPACITY = 4000;
    private static final int TOTAL_MAX_CAPACITY = 20_0000;
    private final Object lock = new Object();
    /**
     * Format: (rule, (value, timeRecorder))
     *
     * @since 1.6.0
     */
    private final Map<ParamFlowRule, CacheMap<Object, RAtomicLong>> ruleTimeCounters =
            new HashMap<>();
    /**
     * Format: (rule, (value, tokenCounter))
     *
     * @since 1.6.0
     */
    private final Map<ParamFlowRule, CacheMap<Object, RAtomicLong>> ruleTokenCounter =
            new HashMap<>();
    private final Map<Integer, CacheMap<Object, RAtomicLong>> threadCountMap = new HashMap<>();
    private final CommandResource ruleTimeCountersCommands =
            RdSupporter.path(PATH_RULE_TIME_COUNTERS);
    private final CommandResource ruleTokenCounterCommands =
            RdSupporter.path(PATH_RULE_TOKEN_COUNTER);
    private final CommandResource threadCountMapCommands =
            RdSupporter.path(PATH_THREAD_COUNT_MAP);

    /**
     * Get the token counter for given parameter rule.
     *
     * @param rule
     *         valid parameter rule
     *
     * @return the associated token counter
     *
     * @since 1.6.0
     */
    public CacheMap<Object, RAtomicLong> getRuleTokenCounter(ParamFlowRule rule) {
        return ruleTokenCounter.get(rule);
    }

    /**
     * Get the time record counter for given parameter rule.
     *
     * @param rule
     *         valid parameter rule
     *
     * @return the associated time counter
     *
     * @since 1.6.0
     */
    public CacheMap<Object, RAtomicLong> getRuleTimeCounter(ParamFlowRule rule) {
        return ruleTimeCounters.get(rule);
    }

    public void clear() {
        synchronized (lock) {
            threadCountMap.clear();
            ruleTimeCounters.clear();
            ruleTokenCounter.clear();
        }
    }

    public void initialize(ParamFlowRule rule) {
        if (!ruleTimeCounters.containsKey(rule)) {
            synchronized (lock) {
                if (ruleTimeCounters.get(rule) == null) {
                    ruleTimeCounters.put(rule, new RedisCacheMap<Object>(ruleTimeCountersCommands));
                }
            }
        }

        if (!ruleTokenCounter.containsKey(rule)) {
            synchronized (lock) {
                if (ruleTokenCounter.get(rule) == null) {
                    ruleTokenCounter.put(rule, new RedisCacheMap<Object>(ruleTokenCounterCommands));
                }

            }
        }

        if (!threadCountMap.containsKey(rule.getParamIdx())) {
            synchronized (lock) {
                if (threadCountMap.get(rule.getParamIdx()) == null) {
                    threadCountMap.put(rule.getParamIdx(),
                            new RedisCacheMap<Object>(threadCountMapCommands));
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public void decreaseThreadCount(Object... args) {
        if (args == null) {
            return;
        }

        try {
            for (int index = 0; index < args.length; index++) {
                CacheMap<Object, RAtomicLong> threadCount = threadCountMap.get(index);
                if (threadCount == null) {
                    continue;
                }

                Object arg = args[index];
                if (arg == null) {
                    continue;
                }
                if (Collection.class.isAssignableFrom(arg.getClass())) {
                    for (Object value : ((Collection) arg)) {
                        decrementAndGet(threadCountMapCommands, threadCount, value);
                    }
                } else if (arg.getClass().isArray()) {
                    int length = Array.getLength(arg);
                    for (int i = 0; i < length; i++) {
                        Object value = Array.get(arg, i);
                        decrementAndGet(threadCountMapCommands, threadCount, value);
                    }
                } else {
                    decrementAndGet(threadCountMapCommands, threadCount, arg);
                }

            }
        } catch (Throwable e) {
            RecordLog.warn("[ParameterMetric] Param exception", e);
        }
    }

    private void decrementAndGet(CommandResource commandResource,
            CacheMap<Object, RAtomicLong> cacheMap, Object value) {
        RAtomicLong rAtomicLong = commandResource.getOrNewAtomicLong(value);
        if (rAtomicLong.get() != 0) {
            long currentValue = rAtomicLong.decrementAndGet();
            if (currentValue <= 0) {
                cacheMap.remove(value);
            }
        }

    }

    private void incrementAndGet(CommandResource commandResource, Object value) {
        RAtomicLong rAtomicLong = commandResource.getOrNewAtomicLong(value);
        rAtomicLong.incrementAndGet();
    }

    @SuppressWarnings("rawtypes")
    public void addThreadCount(Object... args) {
        if (args == null) {
            return;
        }

        try {
            for (int index = 0; index < args.length; index++) {
                CacheMap<Object, RAtomicLong> threadCount = threadCountMap.get(index);
                if (threadCount == null) {
                    continue;
                }

                Object arg = args[index];

                if (arg == null) {
                    continue;
                }

                if (Collection.class.isAssignableFrom(arg.getClass())) {
                    for (Object value : ((Collection) arg)) {
                        incrementAndGet(threadCountMapCommands, value);
                    }
                } else if (arg.getClass().isArray()) {
                    int length = Array.getLength(arg);
                    for (int i = 0; i < length; i++) {
                        Object value = Array.get(arg, i);
                        incrementAndGet(threadCountMapCommands, value);
                    }
                } else {
                    incrementAndGet(threadCountMapCommands, arg);
                }

            }

        } catch (Throwable e) {
            RecordLog.warn("[ParameterMetric] Param exception", e);
        }
    }

    public long getThreadCount(int index, Object value) {
        CacheMap<Object, RAtomicLong> cacheMap = threadCountMap.get(index);
        if (cacheMap == null) {
            return 0;
        }
        RAtomicLong count = cacheMap.get(value);
        return count == null ? 0L : count.get();
    }

    /**
     * Get the token counter map. Package-private for test.
     *
     * @return the token counter map
     */
    Map<ParamFlowRule, CacheMap<Object, RAtomicLong>> getRuleTokenCounterMap() {
        return ruleTokenCounter;
    }

    Map<Integer, CacheMap<Object, RAtomicLong>> getThreadCountMap() {
        return threadCountMap;
    }

    Map<ParamFlowRule, CacheMap<Object, RAtomicLong>> getRuleTimeCounterMap() {
        return ruleTimeCounters;
    }

    public CommandResource getRuleTimeCountersCommands() {
        return ruleTimeCountersCommands;
    }

    public CommandResource getRuleTokenCounterCommands() {
        return ruleTokenCounterCommands;
    }

    public CommandResource getThreadCountMapCommands() {
        return threadCountMapCommands;
    }
}
