package com.refactor.trace;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestURL {
    private String path;
    private String scheme;
    private String port;
    private String domain;
    private String query;
    private String full;
}
