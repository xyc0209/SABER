package com.refactor.config;

import lombok.Data;
import lombok.ToString;
import org.apache.maven.model.Dependency;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Cocoicobird
 * @version 1.0
 * 依赖信息路径
 */
@Data
@ToString
@Component
@ConfigurationProperties(prefix = "refactor.dependencies")
public class DependenciesConfig {
    Dependency eurekaServer;
    Dependency eurekaClient;
    Dependency springCloud;
    Dependency springCloudAlibaba;
    Dependency nacos;
    Dependency configServer;
    Dependency configClient;
    Dependency bootstrap;
    Dependency gateway;
}
