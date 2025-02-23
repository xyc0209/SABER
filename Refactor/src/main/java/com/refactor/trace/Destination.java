package com.refactor.trace;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Destination {
    @JsonProperty("address")
    private String address;
    @JsonProperty("port")
    private Integer port;
    @JsonProperty("ip")
    private String ip;
}
