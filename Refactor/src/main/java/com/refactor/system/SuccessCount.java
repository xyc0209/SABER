package com.refactor.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SuccessCount {
    /**
     * the successful transaction(request) number
     */
    @JsonProperty("sum")
    private Integer sum;
    /**
     * total transaction(request) number
     */
    @JsonProperty("value_count")
    private Integer valueCount;
}
