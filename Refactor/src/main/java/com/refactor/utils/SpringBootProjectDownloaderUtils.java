package com.refactor.utils;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Cocoicobird
 * @version 1.0
 */
public class SpringBootProjectDownloaderUtils {

    private static final String springBootProjectGenerator = "https://start.aliyun.com/";

    /**
     * 利用阿里云 Spring Boot 项目生成器下载一个项目
     * @param projectName 项目名称
     * @param groupId 工程组
     * @param artifactId 名称
     * @param packageName 源文件包名
     * @param dependencies 初始依赖项
     */
    public static void downloadProject(String projectName,
                                       String groupId, String artifactId, String packageName,
                                       List<String> dependencies, String targetDirectory) throws IOException, XmlPullParserException {
        String url = springBootProjectGenerator
                + "starter.zip?type=maven-project" + "&language=java" + "&architecture=none"
                + "&bootVersion=2.6.13" + "&baseDir=" + projectName + "&groupId=" + groupId
                + "&artifactId=" + artifactId + "&name=" + projectName
                + "&packageType=jar" + "&packageName=" + packageName + "&javaVersion=1.8";
        if (dependencies != null && !dependencies.isEmpty()) {
            url += "&dependencies=" + String.join(",", dependencies);
        }
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = client.execute(request)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    File projectFile = new File(projectName + ".zip");
                    System.out.println(projectFile.getAbsolutePath());
                    try (FileOutputStream fos = new FileOutputStream(projectFile)) {
                        fos.write(EntityUtils.toByteArray(entity));
                    }
                    unzipProject(projectFile, targetDirectory);
                }
            }
        }
        MavenParserUtils.deleteUselessConfig(targetDirectory + File.separator + projectName);
        // FileUtils.deleteFilesByPattern(targetDirectory, "properties");
    }

    /**
     * 解压项目
     * @param projectFile 待解压的项目文件
     * @param targetDirectory 解压目录
     */
    public static void unzipProject(File projectFile, String targetDirectory) throws IOException {
        Path targetPath = Paths.get(targetDirectory);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(projectFile.toPath()))) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                Path path = targetPath.resolve(zipEntry.getName());
                if (!zipEntry.isDirectory()) {
                    Files.createDirectories(path.getParent());
                    try (OutputStream os = Files.newOutputStream(path)) {
                        byte[] buffer = new byte[1024];
                        int read;
                        while ((read = zis.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                        }
                    }
                } else {
                    Files.createDirectories(path);
                }
            }
        }
    }
}
