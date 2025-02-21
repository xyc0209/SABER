package com.refactor.trace;

import lombok.Data;

import java.util.Map;

/**
 * @description:
 * @author: xyc
 * @date: 2024-11-29 10:57
 */
@Data
public class TraceDetail {
    private double avgcallPR;
    private Map<String, Map<String, Integer>> callNumMapPR;
}