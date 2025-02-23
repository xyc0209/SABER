package com.refactor.dto;

/**
 * @description:
 * @author: xyc
 * @date: 2025-02-25 16:07
 */
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "elastic.elasticsearch")
@Data
public class ElasticsearchSettings {
    @Schema(description = "用户名", example = "elastic")
    private String userName;
    @Schema(description = "密码", example = "changeme")
    private String password;
    @Schema(description = "Elasticsearch集群/单实例的主机地址", example = "http://10.245.1.233:9200,http://10.245.1.135:9200")
    private String addresses;


}