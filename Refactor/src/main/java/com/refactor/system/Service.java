package com.refactor.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Service {

    @JsonProperty("language")
    private Language language;
    @JsonProperty("name")
    private String name;
    @JsonProperty("node")
    private Node node;
    @JsonProperty("runtime")
    private Runtime runtime;
    @JsonProperty("target")
    private ServiceTarget target;

    private Framework framework;
}
