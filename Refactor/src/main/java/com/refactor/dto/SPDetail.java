package com.refactor.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * @description:
 * @author: xyc
 * @date: 2024-11-22 11:17
 */
@Data
public class SPDetail {
    public String svc1;
    public String svc2;
    public Set<String> sharedEntityList;

    public SPDetail(String svc1, String svc2, Set<String> sharedEntityList) {
        this.svc1 = svc1;
        this.svc2 = svc2;
        this.sharedEntityList = sharedEntityList;
    }
}