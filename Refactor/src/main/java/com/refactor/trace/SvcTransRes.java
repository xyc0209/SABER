package com.refactor.trace;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
/**
 * @description: Data collected under a certain time window corresponding to each service instance
 * @author: xyc
 * @date: 2024-11-28 14:19
 */
@Data
public class SvcTransRes {

    /**
     * date format: yyyy-MM-dd HH:mm:ss
     */
    private Date startTime;
    private Date endTime;
    /**
     * interval equals to startTime - endTime
     * unit: min
     */
    private Integer interval;
    /**
     * unique id: language-serviceName-podName
     */
    private String language;
    private String serviceName;
    /**
     * 需要设置 ELASTIC_APM_SERVICE_NODE_NAME 保证所有数据所属服务实例的唯一性 不产生混淆
     * 因为可能存在同一服务器部署同一服务的多个实例，造成数据混乱
     */
    private String podName;
    private Integer requestCount;

    /**
     * successful request count
     */
    private Integer reqSucCount;

    /**
     * failure request count with 5xx
     */
    private Integer reqServerFailCount;

    /**
     * failure request count with 4xx
     */
    private Integer resClientFailCount;
    /**
     * proportion of fail request to the total without 4xx
     */
    private Double failPercent;
    /**
     * average latency of all requests(processed successfully with error/fail requests) in this time window
     *
     */
    private Double avgLatency;

    /**
     * record requests in per minute
     * from startTime to endTime, record requests number per minute
     * The interval format is inclusive of the left boundary and exclusive of the right boundary, denoted as [left, right)
     */
    private int[] throughput;

    /**
     * record successful requests in per minute
     *
     */
    private int[] sucThroughput;

    /**
     * record call number of every API in this instance(service)
     * key: APIName recorded in Transaction
     * value: call number
     * 指标收集：callAPINumMap
     */
    private Map<String,Integer> instanceAPICallNumMap;

    /**
     * record the service instance invoked by this service instance
     * key: language-serviceName-podName, unique identifier
     * value: call number
     * 指标收集器：serviceCall
     */
    private Map<String, Integer> serviceCallNumMap;

    /**
     * record average execute time of each API invoked during this interval
     * key: APIName
     * value: average execution time
     * 指标收集器：executionTimeMapPer
     */
    private Map<String,Double> APIExecTimeMap;


    /**
     * record the number of API calls and the tree structure of the chain requests between this interval
     */
    private List<RequestChain> requestChainList;

}