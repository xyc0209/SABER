package com.refactor.executor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @description:
 * @author: xyc
 * @date: 2024-10-15 17:48
 */
@Data
@NoArgsConstructor
public class RequestItem {
    private List<String> serviceList;
    private String namespace;
    //can be replaced with annotation @AllArgsConstructor
    public RequestItem(List<String> serviceList){
        this.serviceList = serviceList;
        this.namespace = namespace;
    }

}