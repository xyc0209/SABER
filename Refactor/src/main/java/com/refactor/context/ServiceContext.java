package com.refactor.context;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * @description:
 * @author: xyc
 * @date: 2023-03-30 14:52
 */
@Data
public class ServiceContext {
    public boolean status;
    public Set<String> serviceList;
    public ServiceContext(){
        serviceList = new HashSet<>();
    }
}
