package com.refactor.dto.span;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpanComposite {
    private Integer count;
    @JsonProperty("sum")
    private SumObject sum;
}
