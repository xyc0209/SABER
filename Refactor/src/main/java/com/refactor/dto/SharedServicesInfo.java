package com.refactor.dto;

import lombok.Data;

import java.util.Set;

@Data
public class SharedServicesInfo {
    private String serviceName1; // 服务名称1
    private String serviceName2; // 服务名称2
    private Set<String> commonEntities; // 共享的实体集合

    public SharedServicesInfo(String serviceName1, String serviceName2, Set<String> commonEntities) {
        this.serviceName1 = serviceName1;
        this.serviceName2 = serviceName2;
        this.commonEntities = commonEntities;
    }
}