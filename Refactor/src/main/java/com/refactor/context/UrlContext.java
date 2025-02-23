package com.refactor.context;

import lombok.Data;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Cocoicobird
 * @version 1.0
 * @description 存储一个控制器类中的 url
 * url1 为类注解的 url
 * url2 为方法注解的 url
 * fullQualifiedName 为类的全限定名
 */
@Data
public class UrlContext implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fullQualifiedName;
    private String url1;
    private Map<String, String> url2; // key 为方法名 value 为方法注解的 url
    private Map<String, String> httpMethod; // key 为方法名 value 为方法对应的 http 方法

    public UrlContext() {
        this.url2 = new LinkedHashMap<>();
        this.httpMethod = new LinkedHashMap<>();
    }

    public Boolean isEmpty() {
        return this.url1 == null && this.url2.isEmpty();
    }
}
