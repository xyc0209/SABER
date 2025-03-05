package com.refactor.system;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    @JsonProperty("agent_id_status")
    private String agentIdStatus;
    @JsonProperty("ingested")
    private Date ingested;
    @JsonProperty("success_count")
    private SuccessCount successCount;
}
