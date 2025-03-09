package com.refactor.detail;

import com.refactor.context.SystemMavenInfo;
import com.refactor.detail.model.CodeModification;
import com.refactor.enumeration.ModificationType;
import com.refactor.enumeration.RefactorType;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Cocoicobird
 * @version 1.0
 */
public class ModificationRecorder {
    private SystemMavenInfo systemMavenInfo;
    private RefactorType refactorType;
    private final Map<String, List<CodeModification>> records = new LinkedHashMap<>();

    public ModificationRecorder() {

    }

    public ModificationRecorder(SystemMavenInfo systemMavenInfo, RefactorType refactorType) {
        this.systemMavenInfo = systemMavenInfo;
        this.refactorType = refactorType;
    }

    public void addRecord(String moduleName, CodeModification modification) {
        records.computeIfAbsent(moduleName, k -> new ArrayList<>()).add(modification);
    }

    public Map<String, List<CodeModification>> getRecords() {
        return new LinkedHashMap<>(records);
    }

    public String formatRecords() {
        StringBuilder sb = new StringBuilder();
        appendSystemInfo(sb);
        sb.append("\nREFACTOR STEPS\n");
        sb.append("=====================\n\n");
        List<Map.Entry<String, CodeModification>> sortedEntries = records.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue()
                        .stream()
                        .map(mod -> new AbstractMap.SimpleEntry<>(entry.getKey(), mod)))
                .sorted(Comparator.comparing(entry -> entry.getValue().getTimestamp()))
                .collect(Collectors.toList());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int stepNumber = 1;
        for (Map.Entry<String, CodeModification> entry : sortedEntries) {
            String module = entry.getKey();
            CodeModification mod = entry.getValue();

            String formatted = mod.getTimestamp().format(formatter);
            sb.append(String.format("Step %d. [%s] %s\n",
                    stepNumber++,
                    formatted,
                    module.toUpperCase()));

            sb.append(String.format("%-12s: %s\n", "Type", mod.getType()));
            sb.append(String.format("%-12s: %s\n", "Description", mod.getDescription()));
            sb.append(String.format("%-12s: %s\n", "Old Value", abbreviate(mod.getOldValue())));
            sb.append(String.format("%-12s: %s\n", "New Value", abbreviate(mod.getNewValue())));
            sb.append(String.format("%-12s: %s\n", "Changed File", mod.getFilePath()));
            sb.append("────────────────────────────────────────\n");
        }
        return sb.toString();
    }

    private String abbreviate(String value) {
        return value != null ?
                value : "null";
    }

    private void appendSystemInfo(StringBuilder sb) {
        sb.append("SYSTEM INFORMATION\n");
        sb.append("──────────────────\n");
        sb.append(String.format("%-12s: %s\n", "Group ID", safeGet(systemMavenInfo.getGroupId())));
        sb.append(String.format("%-12s: %s\n", "Artifact ID", safeGet(systemMavenInfo.getArtifactId())));
        sb.append(String.format("%-12s: %s\n", "Version", safeGet(systemMavenInfo.getVersion())));
        sb.append(String.format("%-12s: %s\n", "Java", safeGet(systemMavenInfo.getJavaVersion())));
        sb.append(String.format("%-12s: %s\n", "Spring Boot Version", safeGet(systemMavenInfo.getSpringBootVersion())));
        sb.append(String.format("%-12s: %s\n", "Refactor Type", safeGet(refactorType.name())));
    }

    private String safeGet(String value) {
        return value != null ? value : "N/A";
    }
//
//    public static void main(String[] args) {
//        SystemMavenInfo systemMavenInfo1 = new SystemMavenInfo("com.hitwh", "refactor", "1.0", "1.8", "2.0.0.RELEASE");
//        ModificationRecorder recorder = new ModificationRecorder(systemMavenInfo1, RefactorType.No_Service_Discovery_Pattern);
//        recorder.addRecord("Module1", new CodeModification("file1", ModificationType.DEPENDENCY_ADD, "old1", "new1", "description1"));
//        recorder.addRecord("Module2", new CodeModification("file2", ModificationType.DEPENDENCY_ADD, "old2", "new2", "description2"));
//        System.out.println(recorder.formatRecords());
//    }
}
