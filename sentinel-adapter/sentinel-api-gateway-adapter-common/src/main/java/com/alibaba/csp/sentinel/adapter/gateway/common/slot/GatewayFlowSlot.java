/*
 * Copyright 1999-2019 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.adapter.gateway.common.slot;

import java.util.List;

import com.alibaba.csp.sentinel.adapter.gateway.common.PriorityProperties;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.slotchain.AbstractLinkedProcessorSlot;
import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
import com.alibaba.csp.sentinel.slotchain.StringResourceWrapper;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowChecker;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParameterMetricStorage;

/**
 * @author Eric Zhao
 * @since 1.6.1
 */
public class GatewayFlowSlot extends AbstractLinkedProcessorSlot<DefaultNode> {

    public GatewayFlowSlot() {
        RecordLog.info("Init GatewayFlowSlot");
    }

    @Override
    public void entry(Context context, ResourceWrapper resource, DefaultNode node, int count, boolean prioritized,
            Object... args) throws Throwable {
        checkGatewayParamFlow(resource, count, args);

        fireEntry(context, resource, node, count, prioritized, args);
    }

    private void checkGatewayParamFlow(ResourceWrapper resourceWrapper, int count, Object... args)
            throws BlockException {
        if (args == null) {
            return;
        }

        List<ParamFlowRule> rules = GatewayRuleManager.getConvertedParamRules(resourceWrapper.getName());
        if (rules == null || rules.isEmpty()) {
            return;
        }

        for (ParamFlowRule rule : rules) {
            // Initialize the parameter metrics.
            ParameterMetricStorage.initParamMetricsFor(resourceWrapper, rule);

            if (!ParamFlowChecker.passCheck(resourceWrapper, rule, count, args)) {
                // 优先级启用且存在备用规则标记
                if (loadBackUpsRules(resourceWrapper, count, rule, args)) {
                    return;
                }
                String triggeredParam = "";
                if (args.length > rule.getParamIdx()) {
                    Object value = args[rule.getParamIdx()];
                    triggeredParam = String.valueOf(value);
                }
                throw new ParamFlowException(resourceWrapper.getName(), triggeredParam, rule);
            }
        }
    }

    private boolean loadBackUpsRules(ResourceWrapper resourceWrapper, int count, ParamFlowRule rule, Object[] args)
            throws ParamFlowException {
        if (PriorityProperties.ENABLE && PriorityProperties.RESOURCE_SUFFIX.equals(args[args.length - 1])) {
            // 启用备用规则
            StringResourceWrapper backUpsResource = new StringResourceWrapper(
                    resourceWrapper.getName() + PriorityProperties.RESOURCE_SUFFIX, resourceWrapper.getEntryType());
            List<ParamFlowRule> backUpsRules = GatewayRuleManager.getConvertedParamRules(backUpsResource.getName());
            if (backUpsRules == null || backUpsRules.isEmpty()) {
                throw new RuntimeException("BackUps rules can not be empty");
            }
            for (ParamFlowRule backUpsRule : backUpsRules) {
                ParameterMetricStorage.initParamMetricsFor(backUpsResource, backUpsRule);
                // 根据备用规则和正常规则校验方式一致，所以公用args参数
                if (!ParamFlowChecker.passCheck(backUpsResource, backUpsRule, count, args)) {
                    String triggeredParam = "";
                    if (args.length > rule.getParamIdx()) {
                        Object value = args[rule.getParamIdx()];
                        triggeredParam = String.valueOf(value);
                    }
                    throw new ParamFlowException(resourceWrapper.getName(), triggeredParam, rule);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void exit(Context context, ResourceWrapper resourceWrapper, int count, Object... args) {
        fireExit(context, resourceWrapper, count, args);
    }
}
