package com.refactor.chain.analyzer.config;

import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TypeSolverConfig {
    public static CombinedTypeSolver configureTypeSolver(String projectSrcPath, String localMavenRepo, String moduleSrcPath) throws IOException {
        // 创建 CombinedTypeSolver
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();

        // 1. 解析 Java 标准库
        combinedTypeSolver.add(new ReflectionTypeSolver());

        // 2. 解析项目源码
        combinedTypeSolver.add(new JavaParserTypeSolver(new File(projectSrcPath)));

        // 3. 解析本地 Maven 仓库中的依赖 JAR 文件 和模块依赖的子模块
        // 3.1 解析项目依赖的 JAR 文件的本地路径
        List<String> projectJarPaths = resolveDependenciesJarPath(projectSrcPath, localMavenRepo);
        System.out.println(projectJarPaths.toString());
        // 3.2 解析模块依赖的子模块及依赖的 JAR 文件的本地路径
        List<String> moduleJarPaths = resolveDependenciesJarPath(moduleSrcPath, localMavenRepo);
        assert projectJarPaths != null;
        List<String> allJarPaths = new ArrayList<>(projectJarPaths);
        assert moduleJarPaths != null;

        // 3.3 合并项目依赖的 JAR 文件和模块依赖的 JAR 文件
        allJarPaths.addAll(moduleJarPaths);

        // 3.4 添加到 CombinedTypeSolver
        for (String jarPath : allJarPaths) {
            combinedTypeSolver.add(new JarTypeSolver(jarPath));
        }

        // 4. 解析模块源码
        combinedTypeSolver.add(new JavaParserTypeSolver(new File(moduleSrcPath)));

        return combinedTypeSolver;
    }

    private static List<String> resolveDependenciesJarPath(String srcPath, String localMavenRepo) {
        // 请将以下 savePath 设置为一个合适的路径
        // 如果是在 Windows 系统上，以下的 “\\” 不用改动
        // 如果是在 Linux 或 macos 系统上，请将所有的 “\\” 替换为 “/”
        String savePath = "D:\\calling_sequence_analyzer\\MBS_calling_seq_analyzer\\src\\main\\resources\\dependencies-path.txt";
        try {
            // 构建命令
            ProcessBuilder builder = new ProcessBuilder();
            // 执行命令 mvn dependency:build-classpath -Dmaven.repo.local=<localMavenRepo> -Dmdep.outputFile=<savePath>
            // 请确保 mvn 命令在系统的环境变量路径中
            String[] command = {
                    "D:\\software\\apache-maven-3.6.3\\bin\\mvn.cmd",
                    "-N",
                    "dependency:build-classpath",
                    "-Dmaven.repo.local=" + localMavenRepo,
                    "-Dmdep.outputFile=" + savePath
            };
            builder = new ProcessBuilder(command);
            System.out.println("srcPath"+srcPath);
            // 设置工作目录为 Maven 项目的根目录
            builder.directory(new File(srcPath));

            // 启动进程
            Process process = builder.start();

            // 读取标准输出
            BufferedReader stdOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = stdOutput.readLine()) != null) {
                System.out.println(line);
            }

            Pattern pattern = Pattern.compile("dependency: org.services:([^:]+):jar:0.1.0 (compile)");
            // 读取标准错误
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = stdError.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                System.err.println(line);
                if (matcher.find()) {
                    String projectName = matcher.group(1); // 提取项目名称
                    System.out.println("捕获的项目名称: " + projectName);
                }
            }

            // 等待进程执行完成并获取退出状态
            int exitCode = process.waitFor();
            System.out.println("命令执行完成，退出状态: " + exitCode);

            File jarPath = new File(savePath);
            BufferedReader bufferedReader = new BufferedReader(new FileReader(jarPath));
            String jarContent = bufferedReader.readLine();
            List<String> jarList= (jarContent != null)? new ArrayList<>(Arrays.asList(jarContent.split(";"))) : new ArrayList<>();
            System.out.println(jarList.toString());
            return jarList;


        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
