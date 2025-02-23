package com.refactor.utils;

import com.refactor.context.SystemMavenInfo;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * @author Cocoicobird
 * @version 1.0
 */
public class MavenParserUtils {

    private static final MavenXpp3Reader reader = new MavenXpp3Reader();
    private static final MavenXpp3Writer writer = new MavenXpp3Writer();

    public static Model getMavenModel(String mavenPath) throws IOException, XmlPullParserException {
        try (FileReader fr = new FileReader(mavenPath)) {
            return reader.read(fr);
        }
    }

    public static String getMavenArtifactId(String mavenPath) throws IOException, XmlPullParserException {
        try (FileReader fr = new FileReader(mavenPath)) {
            Model model = reader.read(fr);
            return model.getArtifactId();
        }
    }

    public static List<Dependency> getMavenDependencies(String mavenPath) throws IOException, XmlPullParserException {
        try (FileReader fr = new FileReader(mavenPath)) {
            Model model = reader.read(fr);
            return model.getDependencies();
        }
    }

    /**
     * 添加一个依赖
     * @param mavenPath maven 文件路径
     * @param dependency 新依赖
     */
    public static void addMavenDependencies(String mavenPath, Dependency dependency) throws IOException, XmlPullParserException {
        try (FileReader fr = new FileReader(mavenPath)) {
            Model model = reader.read(fr);
            List<Dependency> dependencies = model.getDependencies();
            boolean isExist = false;
            for (Dependency d : dependencies) {
                if (d.getGroupId().equals(dependency.getGroupId()) && d.getArtifactId().equals(dependency.getArtifactId())) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist) {
                dependencies.add(dependency);
            }
            model.setDependencies(dependencies);
            try (FileWriter fw = new FileWriter(mavenPath)) {
                writer.write(fw, model);
            }
        }
    }

    /**
     * 添加多个依赖
     * @param mavenPath maven 文件路径
     * @param dependencies 新依赖
     */
    public static void addMavenDependencies(String mavenPath, List<Dependency> dependencies) throws IOException, XmlPullParserException {
        try (FileReader fr = new FileReader(mavenPath)) {
            Model model = reader.read(fr);
            List<Dependency> dependencyList = model.getDependencies();
            for (Dependency d : dependencies) {
                boolean isExist = false;
                for (Dependency dependency : dependencyList) {
                    if (dependency.getGroupId().equals(d.getGroupId()) && dependency.getArtifactId().equals(d.getArtifactId())) {
                        isExist = true;
                        break;
                    }
                }
                if (!isExist) {
                    dependencyList.add(d);
                }
            }
            model.setDependencies(dependencyList);
            try (FileWriter fw = new FileWriter(mavenPath)) {
                writer.write(fw, model);
            }
        }
    }

    /**
     * 修改 Spring Boot 版本号
     * @param mavenPath maven 文件路径
     * @param springBootVersion Spring Boot 版本号
     */
    public static void updateSpringBootVersion(String mavenPath, String springBootVersion) throws IOException, XmlPullParserException {
        try (FileReader fr = new FileReader(mavenPath)) {
            Model model = reader.read(fr);
            model.getProperties().setProperty("spring-boot.version", springBootVersion);
            try (FileWriter fw = new FileWriter(mavenPath)) {
                writer.write(fw, model);
            }
        }
    }

    public static void setParent(String mavenPath, String parentGroupId, String parentArtifactId, String parentVersion) throws IOException, XmlPullParserException {
        try (FileReader fr = new FileReader(mavenPath)) {
            Model model = reader.read(fr);
            Parent parent = new Parent();
            parent.setGroupId(parentGroupId);
            parent.setArtifactId(parentArtifactId);
            parent.setVersion(parentVersion);
            model.setParent(parent);
            try (FileWriter fw = new FileWriter(mavenPath)) {
                writer.write(fw, model);
            }
        }
    }

    public static void addModule(String mavenPath, String moduleName) throws IOException, XmlPullParserException {
        try (FileReader fr = new FileReader(mavenPath)) {
            Model model = reader.read(fr);
            List<String> modules = model.getModules();
            boolean isExist = false;
            for (String module : modules) {
                if (module.equals(moduleName)) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist) {
                modules.add(moduleName);
            }
            model.setModules(modules);
            try (FileWriter fw = new FileWriter(mavenPath)) {
                writer.write(fw, model);
            }
        }
    }

    public static boolean hasSpringCloud(String mavenPath) throws XmlPullParserException, IOException {
        try (FileReader fr = new FileReader(mavenPath)) {
            Model model = reader.read(fr);
            List<Dependency> dependencies = model.getDependencies();
            for (Dependency dependency : dependencies) {
                if ("org.springframework.cloud".equals(dependency.getGroupId())
                        && "spring-cloud-dependencies".equals(dependency.getArtifactId())) {
                    return true;
                }
            }
            return false;
        }
    }

    public static SystemMavenInfo getSystemMavenInfo(String mavenPath) throws XmlPullParserException, IOException {
        String parentPomXml = mavenPath + "/pom.xml";
        Model parentMavenModel = MavenParserUtils.getMavenModel(parentPomXml);
        // 获取系统 pom 信息
        String groupId = parentMavenModel.getGroupId();
        String artifactId = parentMavenModel.getArtifactId();
        String version = parentMavenModel.getVersion();
        String javaVersion = parentMavenModel.getProperties().getProperty("java.version");
        String springBootVersion = parentMavenModel.getProperties().getProperty("spring-boot.version");
        return new SystemMavenInfo(groupId, artifactId, version, javaVersion, springBootVersion);
    }
}
