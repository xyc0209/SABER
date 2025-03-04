package com.refactor.dto;

/**
 * @description:
 * @author: xyc
 * @date: 2025-02-25 16:15
 */
import lombok.Data;

@Data
public class Host {
    private String hostName;
    private Integer port;
}