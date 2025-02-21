package com.refactor.utils;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * @description:
 * @author: xyc
 * @date: 2024-11-28 18:24
 */
public class DependencyUtils {
    private static final String SERVICE_POM_SUFFIX = "service";
    public static void mergeDependencies(String nanoPath, String normalPath) throws IOException {
        File nanoPomFile = new File(nanoPath, "pom.xml");
        File normalPomFile = new File(normalPath, "pom.xml");

        if (!nanoPomFile.exists() || !normalPomFile.exists()) {
            throw new IllegalArgumentException("POM files must exist.");
        }

        MavenXpp3Reader reader = new MavenXpp3Reader();
        MavenXpp3Writer writer = new MavenXpp3Writer();

        try (FileReader nanoReader = new FileReader(nanoPomFile);
             FileReader normalReader = new FileReader(normalPomFile)) {

            // 读取 nano 的 pom.xml
            Model nanoModel = reader.read(nanoReader);
            // 读取 normal 的 pom.xml
            Model normalModel = reader.read(normalReader);

            // 获取 nano 的 properties
            Properties nanoProperties = nanoModel.getProperties();
            // 获取 normal 的 properties
            Properties normalProperties = normalModel.getProperties();

            // 遍历 nano 的 properties，检查并添加到 normal 的 properties
            for (String key : nanoProperties.stringPropertyNames()) {
                if (!normalProperties.containsKey(key)) {
                    normalProperties.setProperty(key, nanoProperties.getProperty(key));
                }
            }

            // 获取 nano 的依赖
            List<Dependency> nanoDependencies = nanoModel.getDependencies();
            List<Dependency> normalDependencies = normalModel.getDependencies();



            for (Dependency nanoDep : nanoDependencies) {
                String groupId = nanoDep.getGroupId();
                String artifactId = nanoDep.getArtifactId();
                System.out.println("groupId" +groupId);
                System.out.println("artifactId" + artifactId);
                // 检查 normalPOM 是否已包含该依赖
                if (!dependencyExists(normalDependencies, groupId, artifactId)) {
                    // 如果不存在，则添加该依赖
                    System.out.println(nanoDep.toString());
                    normalModel.addDependency(nanoDep);
                }
            }

            // 保存修改后的 normal pom.xml
            try (FileWriter normalWriter = new FileWriter(normalPomFile)) {
                writer.write(normalWriter, normalModel);
            }

        } catch (Exception e) {
            throw new IOException("Error processing POM files", e);
        }
    }
    public static String getVserion(String servicePath) throws IOException, XmlPullParserException {
        System.out.println("servicePath" +servicePath);
        File pomFile = new File(servicePath, "pom.xml");

        if (!pomFile.exists()) {
            throw new IllegalArgumentException("POM files must exist.");
        }

        MavenXpp3Reader reader = new MavenXpp3Reader();

        FileReader nanoReader = new FileReader(pomFile);

        Model model = reader.read(nanoReader);

        // 获取 nano 的 properties
        String version = model.getVersion();
        System.out.println(version);
        return version;

    }
    private static boolean dependencyExists(List<Dependency> dependencies, String groupId, String artifactId) {
        for (Dependency dep : dependencies) {
            if (dep.getGroupId().equals(groupId) && dep.getArtifactId().equals(artifactId)) {
                return true;
            }
        }
        return false;
    }

    public static void modifyPomFile(File pomFile) throws IOException, XmlPullParserException {
        String POM_FILE_PATH = pomFile.getAbsolutePath();
        FileReader fileReader = new FileReader(POM_FILE_PATH);
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(fileReader);
        fileReader.close();

        boolean isServicePom = false;
        String artifactId = model.getArtifactId();
        if (artifactId.toLowerCase().endsWith(SERVICE_POM_SUFFIX)) {
            isServicePom = true;
        }

        if (isServicePom) {
            Dependency clientDependency = createDependency("io.kubernetes", "client-java", "20.0.0", null);
            Dependency apiDependency = createDependency("io.kubernetes", "client-java-api", "20.0.0",null);
            Dependency loadbalancerDependency = createDependency("org.springframework.cloud", "spring-cloud-starter-loadbalancer", "2.2.1.RELEASE",null);

            model.addDependency(clientDependency);
            model.addDependency(apiDependency);
            model.addDependency(loadbalancerDependency);

            FileWriter fileWriter = new FileWriter(POM_FILE_PATH);
            MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(fileWriter, model);
            fileWriter.close();
        }
    }
    private static Dependency createDependency(String groupId, String artifactId, String version, String scope) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        if (scope != null)
            dependency.setScope(scope);
        return dependency;
    }


    public static void main(String[] args) throws XmlPullParserException, IOException {
        getVserion("D:\\code\\demo-collection\\Service-Demo\\testService");
//        try {
//            mergeDependencies("D:\\code\\demo-collection\\nano-test\\travelservice", "D:\\code\\demo-collection\\nano-test\\routeservice");
//            System.out.println("Dependencies merged successfully.");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}