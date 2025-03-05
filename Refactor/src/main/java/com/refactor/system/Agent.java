package com.refactor.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Agent {
    @JsonProperty("name")
    private String name;
    @JsonProperty("version")
    private String version;
}
