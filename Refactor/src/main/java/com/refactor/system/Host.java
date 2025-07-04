package com.refactor.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Host {
    @JsonProperty("architecture")
    private String architecture;

    @JsonProperty("hostname")
    private String hostname;

    @JsonProperty("ip")
    private String[] ip;

    @JsonProperty("name")
    private String name;

    @JsonProperty("os")
    private OperateSystem os;
}
