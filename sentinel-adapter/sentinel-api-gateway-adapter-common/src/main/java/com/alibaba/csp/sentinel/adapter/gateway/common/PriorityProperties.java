package com.alibaba.csp.sentinel.adapter.gateway.common;

/**
 * @author JiangYangfan
 * @version $Id$
 * @since 2019/12/9 10:27
 */
public class PriorityProperties {

    public static final boolean ENABLE = Boolean.parseBoolean(System.getProperty("rap.gateway.flow.enabled", "true"));
    public static final String RESOURCE_VIP_SUFFIX = System.getProperty("rap.gateway.flow.vip.suffix", "!");
    public static final String VIP_HEADER = System.getProperty("rap.gateway.flow.vip.header", "X-GATEWAY-FLOW-VIP");
    public static final String GATEWAY_VIP_PARAM = "$!VIP";
}
