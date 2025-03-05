package com.refactor.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Labels {
    @JsonProperty("area")
    private String area;
    @JsonProperty("id")
    private String id;
}
