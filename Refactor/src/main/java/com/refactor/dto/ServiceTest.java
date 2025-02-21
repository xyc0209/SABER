package com.refactor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @description:
 * @author: xyc
 * @date: 2024-12-14 11:42
 */
@Data
@NoArgsConstructor
public class ServiceTest {
    Map<String, Map<String, String>> serviceDetail;
    List<Set<String>> resultCSCall;
}