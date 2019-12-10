package com.alibaba.csp.sentinel.adapter.gateway.common;

/**
 * @author JiangYangfan
 * @version $Id$
 * @since 2019/12/9 10:27
 */
public class PriorityProperties {

    public static final boolean ENABLE = Boolean
            .parseBoolean(System.getProperty("rap-cloud-gateway.priority.enabled", "true"));
    public static final String RESOURCE_SUFFIX = System.getProperty("rap-cloud-gateway.priority.resource_suffix", "$");
    public static final String HEADER = System.getProperty("rap-cloud-gateway.priority.header", "RAP");
    public static final String HEADER_VALUE = System.getProperty("rap-cloud-gateway.priority.header_value", "fulge");
    public static final String GATEWAY_DEFAULT_PRIORITY_PARAM = "$P";
}
