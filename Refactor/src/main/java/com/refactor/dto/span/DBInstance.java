package com.refactor.dto.span;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DBInstance {
    @JsonProperty("instance")
    private String instance;
    @JsonProperty("statement")
    private String statement;
    @JsonProperty("type")
    private String type;
    @JsonProperty("user.name")
    private String userName;
}
