package com.refactor.detail;

import com.refactor.detail.model.CodeModification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Cocoicobird
 * @version 1.0
 */
public class ModificationRecorder {
    private final Map<String, List<CodeModification>> records = new LinkedHashMap<>();

    public void addRecord(String moduleName, CodeModification modification) {
        records.computeIfAbsent(moduleName, k -> new ArrayList<>()).add(modification);
    }

    public Map<String, List<CodeModification>> getRecords() {
        return new LinkedHashMap<>(records);
    }

    public String formatRecords() {
        StringBuilder sb = new StringBuilder();
        records.forEach((module, mods) -> {
            sb.append("Module: ").append(module).append("\n");
            mods.forEach(mod -> sb.append(
                    String.format("[%s] %s: %s -> %s\n  File: %s\n",
                            mod.getTimestamp(), mod.getType(),
                            abbreviate(mod.getOldValue()), abbreviate(mod.getNewValue()),
                            mod.getFilePath()))
            );
        });
        return sb.toString();
    }

    private String abbreviate(String value) {
        return value != null ?
                (value.length() > 50 ? value.substring(0, 47) + "..." : value) : "null";
    }
}
