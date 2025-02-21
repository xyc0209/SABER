package com.refactor.suggest;

import lombok.Data;

import java.util.Set;

/**
 * @description:
 * @author: xyc
 * @date: 2024-11-22 18:48
 */
@Data
public class RefactorISISuggestion {
    private String databaseName; // 数据库名称
    private Set<String> services; // 受影响的服务集合
    private String suggestion; // 重构建议内容

    public RefactorISISuggestion(String databaseName, Set<String> services, String suggestion) {
        this.databaseName = databaseName;
        this.services = services;
        this.suggestion = suggestion;
    }
}

