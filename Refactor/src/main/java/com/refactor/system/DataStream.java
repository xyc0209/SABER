package com.refactor.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataStream {
    @JsonProperty("dataset")
    private String dataset;
    @JsonProperty("namespace")
    private String namespace;
    @JsonProperty("type")
    private String type;

}
