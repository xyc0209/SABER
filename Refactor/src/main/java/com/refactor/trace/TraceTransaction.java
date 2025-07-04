package com.refactor.trace;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TraceTransaction {

    private String result;
    @JsonProperty("duration")

    private Duration duration;

    @JsonProperty("representative_count")
    private Integer representativeCount;

    private String name;

    private String id;

    @JsonProperty("span_count")
    private SpanCount spanCount;

    private String type;

    private String sampled;
}
