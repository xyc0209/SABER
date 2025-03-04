package com.refactor.trace;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpanCount {
    @JsonProperty("dropped")
    private Integer dropped;
    @JsonProperty("started")
    private Integer started;
}
