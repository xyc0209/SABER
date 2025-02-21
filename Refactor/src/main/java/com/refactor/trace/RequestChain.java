package com.refactor.trace;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @description:
 * @author: xyc
 * @date: 2024-11-28 14:20
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestChain {

    private String traceId;
    private String sourceSvc;
    private String APIName;
    /**
     * 存储当前链路对第一层级服务的调用次数
     * key : language|serviceName|podName|APIName
     * value: times
     */
    private Map<String,Integer> targetSvcNumMap;
    private TraceChainNode chain;
}