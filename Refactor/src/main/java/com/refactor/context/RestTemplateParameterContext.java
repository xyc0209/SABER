package com.refactor.context;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Cocoicobird
 * @version 1.0
 * @description: 存储一些进行参数匹配的信息
 * 1. urls 一个微服务模块下声明的 URL
 * 2. microserviceName 微服务模块的名称
 * 3. 该对象对应的 java 文件中的类变量声明
 */
@Data
public class RestTemplateParameterContext {
    private String microserviceName;
    private List<String> urls;
    private Map<String, StringVariableContext> variableNameAndValues;

    public RestTemplateParameterContext() {
        this.urls = new LinkedList<>();
        this.variableNameAndValues = new LinkedHashMap<>();
    }

    public RestTemplateParameterContext(String microserviceName, List<String> urls, Map<String, StringVariableContext> variableNameAndValues) {
        this.microserviceName = microserviceName;
        this.urls = urls;
        this.variableNameAndValues = variableNameAndValues;
    }
}
