package com.refactor.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: xyc
 * @date: 2024-12-09 19:23
 */
@Data
public class ApiVersionContext {
    public boolean status;
    public Map<String, Map<String, String>> unversionedMap;
    public Map<String, Map<String, String>> missingUrlMap;
    public ApiVersionContext(){
        this.unversionedMap = new HashMap<>();
        this.missingUrlMap = new HashMap<>();
    }

    public void addUnversionedApis(String serviceName, Map<String, String> methodAndApi){
        this.unversionedMap.put(serviceName, methodAndApi);
    }

    public void addMissingUrlMap(String serviceName, Map<String, String> methodAndApi){
        this.missingUrlMap.put(serviceName, methodAndApi);
    }

}