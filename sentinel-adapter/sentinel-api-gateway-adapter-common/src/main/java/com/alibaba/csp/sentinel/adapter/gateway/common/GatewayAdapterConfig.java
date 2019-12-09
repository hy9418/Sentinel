package com.alibaba.csp.sentinel.adapter.gateway.common;

/**
 * @author JiangYangfan
 * @version $Id$
 * @since 2019/12/9 10:27
 */
public class GatewayAdapterConfig {

    public static final boolean PRIORITY_ENABLE = Boolean
            .parseBoolean(System.getProperty("rap.gateway.priority.enable"));
    public static final String STANDBY_RESOURCE_NAME_SUFFIX = System
            .getProperty("rap.gateway.priority.resource_name_suffix", "$");
    public static final String STANDBY_REQUEST_HEADER = System.getProperty("rap.gateway.priority.header.key", "RAP");
    public static final String STANDBY_REQUEST_HEADER_VALUE = System
            .getProperty("rap.gateway.priority.header.value", "ifulge");

    public GatewayAdapterConfig() {
        super();
    }
}
