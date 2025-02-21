package com.refactor.executor.pod;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @description:
 * @author: xyc
 * @date: 2024-12-01 22:15
 */
@Data
@NoArgsConstructor
public class PodInfo {
    private String name;
    private String podIp;
    private String nameSpace;

    private List<ContainersItem> containers;
    private String nodeName;
}