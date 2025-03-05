package com.refactor.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Observer {
    @JsonProperty("hostname")
    private String hostName;
    @JsonProperty("type")
    private String type;
    @JsonProperty("version")
    private String version;
}
