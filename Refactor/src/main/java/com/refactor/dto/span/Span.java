package com.refactor.dto.span;

/**
 * @description:
 * @author: xyc
 * @date: 2025-02-25 16:19
 */
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Span {

    @JsonProperty("duration")
    private SpanDuration duration;
    @JsonProperty("representative_count")
    private Integer representativeCount;
    @JsonProperty("subtype")
    private String subtype;
    @JsonProperty("composite")
    private SpanComposite composite;
    @JsonProperty("destination.service.resource")
    private String destinationResource;
    @JsonProperty("name")
    private String name;
    @JsonProperty("action")
    private String action;
    @JsonProperty("id")
    private String id;
    @JsonProperty("type")
    private String type;
    @JsonProperty("db")
    private DBInstance db;



}