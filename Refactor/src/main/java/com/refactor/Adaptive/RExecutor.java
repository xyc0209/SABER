package com.refactor.Adaptive;

import com.refactor.utils.FileFinder;
import com.refactor.utils.DockerUtils;
import com.refactor.utils.HarborUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class RExecutor {

    @Value("${harbor.host}")
    private  String HARBOR_HOST;
    private static final String HARBOR_PROJECT = "testreposity";
    private static final String HARBOR_USERNAME = "admin";
    private static final String HARBOR_PASSWORD = "Harbor12345";
    private static final String IMAGE_NAME = "your-image-name";
    private static final String IMAGE_TAG = "latest";
    @Value("${harbor.project}")
    private  String harborProject;
    private static final String os = System.getProperty("os.name").toLowerCase();

    public static void main(String[] args) throws IOException, InterruptedException {
        String imageName = "testserviceimage";
//        buildAndPushToHarbor(imageName);

//        String accessToken = getAccessTokenFromDockerLogin(HARBOR_HOST, HARBOR_USERNAME, HARBOR_PASSWORD);
//        System.out.println("accessToken" + accessToken);
    }

    public  void buildAndPushToHarbor(Map.Entry<String, Map<String, String>> svcDetail) {
        System.out.println("HARBOR_HOST"+ HARBOR_HOST);
        try {
            // 切换到 Java 项目路径
            String svcPath = svcDetail.getKey();
            changeDirectory(svcPath);
            String jarPath = "";
            if (os.contains("win"))
                jarPath = svcPath + "\\target";
            else
                jarPath = svcPath + "/target";
            // 使用 Maven 打包 Java 程序
            executeMavenBuild(svcPath);
            System.out.println("JAR PATH" + FileFinder.getLatestJarFilePaths(jarPath));
            FileFinder.getLatestJarFilePaths(jarPath).get(0);
            DockerUtils.generateDockerfile(jarPath, FileFinder.getLatestJarFilePaths(jarPath).get(0), 8089);

            // 构建 Docker 镜像
            String imageName = HARBOR_HOST + "/" + HARBOR_PROJECT + "/" + svcDetail.getValue().get("serviceName").toLowerCase() + ":" + IMAGE_TAG;
            svcDetail.getValue().put("imageName", imageName);
            buildDockerImage(imageName, jarPath);
//            buildDockerImage();
//
//            // 登录到 Harbor
            loginToHarbor(svcPath);
//
//            // 推送 Docker 镜像到 Harbor
            pushImageToHarbor(imageName);


            System.out.println("Build and push to Harbor completed successfully!");
        } catch (IOException | InterruptedException e) {
            System.err.println("Error occurred: " + e.getMessage());
        }
    }


//    private static void changeDirectory(String path) throws IOException, InterruptedException {
//        System.out.println("path"+path);
//        System.out.println(getPathBeforeFirstSeparator(path));
////        executeCommand("cd", getPathBeforeFirstSeparator(path));
//        executeCommand("cd", path, "mvn", "clean", "package");
//    }
    private static void changeDirectory(String path){
        System.setProperty("user.dir", path);
    }

    public static String getPathBeforeFirstSeparator(String path) {
        int firstSeparatorIndex = path.indexOf("\\");
        if (firstSeparatorIndex == -1) {
            // 没有找到反斜杠,尝试找正斜杠
            firstSeparatorIndex = path.indexOf("/");
        }
        if (firstSeparatorIndex == -1) {
            // 没有找到任何分隔符
            return path;
        } else {
            // 获取第一个分隔符之前的部分
            return path.substring(0, firstSeparatorIndex);
        }
    }

    private static void executeMavenBuild(String svcPath) throws IOException, InterruptedException {
        executeCommand(svcPath,"mvn", "clean", "package");
    }

//    private static void buildDockerImage() throws IOException, InterruptedException {
//        executeCommand("docker", "build", "-t", HARBOR_HOST + "/" + HARBOR_PROJECT + "/" + IMAGE_NAME + ":" + IMAGE_TAG, ".");
//    }
    private static void buildDockerImage(String imageName, String dockerfilePath) {
        try {
            executeCommand(dockerfilePath, "docker", "build", "-t", imageName, ".");
        } catch (IOException | InterruptedException e) {
            System.err.println("Error building Docker image: " + e.getMessage());
        }
    }

    private  void loginToHarbor(String svcPath) throws IOException, InterruptedException {
        System.out.println("HARBOR_HOST:" + HARBOR_HOST);
        if(executeCommand(svcPath, "docker", "login", HARBOR_HOST, "-u", HARBOR_USERNAME, "-p", HARBOR_PASSWORD)){
            System.out.println("LOGIN SUCESS");
            HarborUtils.harborProjectExists("http://" + HARBOR_HOST + "/api/v2.0", harborProject);
        }
    }

    private static String getAccessTokenFromDockerLogin(String harborHost, String username, String password) {
        try {
            Process process = new ProcessBuilder("docker", "login", harborHost, "-u", username, "-p", password).start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                // Retrieve the access token from the Docker login response
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    if (line.contains("Token")) {
                        String[] parts = line.split(":");
                        if (parts.length > 1) {
                            return parts[1].trim();
                        }
                    }
                }
            } else {
                System.err.println("Error executing Docker login: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing Docker login: " + e.getMessage());
        }
        return null;
    }
    private static void pushImageToHarbor(String imageName) throws IOException, InterruptedException {
        System.out.println("PUSH SUCESS" + executeCommand("", "docker", "push", imageName));
    }

//    private static void executeCommand(String... command) throws IOException, InterruptedException {
//        List<String> fullCommand = new ArrayList<>();
//        fullCommand.add("cmd.exe");
//        fullCommand.add("/c");
//        System.out.println("Arrays.asList(command)"+Arrays.asList(command));
//        fullCommand.addAll(Arrays.asList(command));
//
//        Process process = new ProcessBuilder(fullCommand).inheritIO().start();
//        int exitCode = process.waitFor();
//        if (exitCode != 0) {
//            throw new IOException("Command failed with exit code: " + exitCode);
//        }
//    }
private static boolean executeCommand(String workingDirectory, String... command) throws IOException, InterruptedException {
    List<String> fullCommand = new ArrayList<>();
    if (os.contains("win")) {
        fullCommand.add("cmd.exe");
        fullCommand.add("/c");
        fullCommand.add("cd");
        fullCommand.add(workingDirectory);
        fullCommand.add("&&");
        fullCommand.addAll(Arrays.asList(command));
    } else {
        // Linux/Unix 系统的命令
        fullCommand = new ArrayList<>();
        fullCommand.add("/bin/bash");
        fullCommand.add("-c");
        fullCommand.add("cd " + workingDirectory + " && " + String.join(" ", command));
    }

    Process process = new ProcessBuilder(fullCommand).inheritIO().start();
    int exitCode = process.waitFor();
    if (exitCode != 0) {
        throw new IOException("Command failed with exit code: " + exitCode);
    }
    return true;
}

}
