package com.refactor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @description:
 * @author: xyc
 * @date: 2024-10-16 22:16
 */
@Data
@NoArgsConstructor
public class ServiceDetail {
    Map<String, Map<String, String>> serviceDetail;
    List<Set<String>> resultCSCall;
    List<ChainKey> chainKeyList;
}