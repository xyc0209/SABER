package com.refactor.suggest;

import lombok.Data;

import java.util.Set;

@Data
public class RefactorSPSuggestion {
    private String databaseName; // 数据库名称
    private Set<String> services; // 受影响的服务集合
    private String suggestion; // 重构建议内容

    public RefactorSPSuggestion(String databaseName, Set<String> services, String suggestion) {
        this.databaseName = databaseName;
        this.services = services;
        this.suggestion = suggestion;
    }
}