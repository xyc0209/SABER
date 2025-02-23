package com.refactor.context;

import lombok.Data;

/**
 * @author Cocoicobird
 * @version 1.0
 */
@Data
public class SystemMavenInfo {
    private String groupId;
    private String artifactId;
    private String version;
    private String javaVersion;
    private String springBootVersion;

    public SystemMavenInfo() {}

    public SystemMavenInfo(String groupId, String artifactId, String version, String javaVersion, String springBootVersion) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.javaVersion = javaVersion;
        this.springBootVersion = springBootVersion;
    }
}
