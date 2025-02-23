package com.refactor.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author Cocoicobird
 * @version 1.0
 * 配置文件模板路径
 */
@Data
@ToString
@Component
@ConfigurationProperties(prefix = "refactor.configuration.templates")
public class TemplatesConfig {
    private String eurekaServer;
    private String eurekaClient;
    private String nacosClient;
    private String configServer;
    private String configClient;
    private String gateway;

    @PostConstruct
    public void print() {
        System.out.println("22222" + this);
    }
}
