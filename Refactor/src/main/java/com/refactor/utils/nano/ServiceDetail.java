package com.refactor.utils.nano;

import lombok.Data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @description: describe coupling, cohesion of class
 * @author: xyc
 * @date: 2024-11-04 19:48
 */
@Data
public class ServiceDetail {
    String name; // 服务 ID
    String servicePath;
    int invocations; // 调用次数
    double coupling; // 耦合度
    double cohesion; // 内聚性
    Map<String, Double> cohesionMap;

    public ServiceDetail(String name, String servicePath, int invocations, double coupling, double cohesion, Map<String, Double> cohesionMap) {
        this.name = name;
        this.servicePath = servicePath;
        this.invocations = invocations;
        this.coupling = coupling;
        this.cohesion = cohesion;
        this.cohesionMap = cohesionMap;
    }


    // NanoService类，继承自Service

}