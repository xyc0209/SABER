package com.refactor.executor.node;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.omg.CORBA.PRIVATE_MEMBER;

import java.math.BigDecimal;

/**
 * @description:
 * @author: xyc
 * @date: 2024-12-01 22:12
 */
@Data
@NoArgsConstructor
public class NodeInfo {
    private String name;
    private String ip;
    private BigDecimal ramUsed;
    private BigDecimal cpuUsed;
    private BigDecimal ramMAX;
    private BigDecimal cpuMax;

    public NodeInfo(String name) {
        this.name = name;
    }
}