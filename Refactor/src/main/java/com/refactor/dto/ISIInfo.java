package com.refactor.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class ISIInfo {
    private String database;
    private List<String> intimacySet;

    public ISIInfo(String database, List<String> intimacySet) {
        this.intimacySet = intimacySet;
    }
}