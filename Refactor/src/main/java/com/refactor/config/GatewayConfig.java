package com.refactor.config;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Cocoicobird
 * @version 1.0
 */
@Data
public class GatewayConfig {
    private String id;
    private String uri;
    private List<String> predicates;

    public GatewayConfig() {
        this.predicates = new LinkedList<>();
    }

    public GatewayConfig(String id, String uri, List<String> predicates) {
        this.id = id;
        this.uri = uri;
        this.predicates = predicates;
    }
}
