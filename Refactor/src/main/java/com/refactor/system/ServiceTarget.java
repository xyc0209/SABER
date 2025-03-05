package com.refactor.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceTarget {
    @JsonProperty("name")
    private String name;
    @JsonProperty("type")
    private String type;
}
