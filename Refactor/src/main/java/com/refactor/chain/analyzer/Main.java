package com.refactor.chain.analyzer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.refactor.chain.analyzer.extension.RepositoryParentExtraction;
import com.refactor.chain.analyzer.init.JavaParserInitializer;
import com.refactor.chain.analyzer.layer.LayerIdentifier;
import com.refactor.chain.analyzer.layer.LayerType;
import com.refactor.chain.analyzer.method.MethodCollector;
import com.refactor.chain.analyzer.parser.CodeParser;
import com.refactor.chain.utils.CallSeqTree;
import com.refactor.chain.utils.Edge;
import com.refactor.chain.utils.Node;
import com.refactor.chain.utils.echart.EChartsServer;
import com.refactor.utils.Community;
import com.refactor.utils.LouvainAlgorithm;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class Main {
    public static void main(String[] args) throws IOException {
//        String savePath = "D:\\calling_sequence_analyzer\\MBS_calling_seq_analyzer\\src\\main\\resources\\dependencies-path.txt";
//        File jarPath = new File(savePath);
//        List<String> dependencyList = Arrays.asList(new BufferedReader(new FileReader(jarPath)).readLine().split(";"));
//        System.out.println(dependencyList);

        /*
         请先浏览 TypeSolverConfig.java 文件，按照注释来配置相关内容
                   请浏览以下内容，按照注释来配置相关内容

        |--------------------------------------------------------|
        |                                                        |
        |    确保已在项目父模块中执行完成 mvn clean install 后再执行 |
        |                                                        |
        |--------------------------------------------------------|

        */
        // 使用时请先修改以下三个路径为自己本地的路径
        // 设置项目源码路径
//        String projectSrcPath = "D:\\code\\demo-collection\\Service-Demo"; // 修改为项目父模块根路径
//        // 设置本地maven仓库路径
//        String localMavenRepo = "D:\\maven-reposity";
//        // 设置模块源码根路径（包含pom文件的路径）
//        String moduleSrcPath = "D:\\code\\demo-collection\\Service-Demo\\testService";
//        // delombok 模块源码，获取 delomboked 源码路径
//        String delombokedModuleSrcPath = getDelombokedSrcPath(moduleSrcPath);
//        // 初始化 JavaParser
//        JavaParser parser = JavaParserInitializer.initializeParser(projectSrcPath, localMavenRepo, delombokedModuleSrcPath);
//
//        // 解析项目代码
//        CodeParser codeParser = new CodeParser(parser);
//        List<CompilationUnit> projectCompilationUnits = codeParser.parseProject(projectSrcPath);
//        assert delombokedModuleSrcPath != null;
//        List<CompilationUnit> moduleCompilationUnits = codeParser.parseProject(delombokedModuleSrcPath);
//
//        System.out.println("项目解析完成，共解析了 " + projectCompilationUnits.size() + " 个文件。");
//        System.out.println("模块解析完成，共解析了 " + moduleCompilationUnits.size() + " 个文件。");
//
//        // 识别层
//        LayerIdentifier layerIdentifier = new LayerIdentifier();
//        Map<String, LayerType> classLayerMap = layerIdentifier.identifyLayers(moduleCompilationUnits);
//
//        // 收集方法
//        MethodCollector methodCollector = new MethodCollector();
//        Map<String, List<com.github.javaparser.ast.body.MethodDeclaration>> classMethodsMap = methodCollector.collectMethods(moduleCompilationUnits, classLayerMap);
//
//        // 获取继承关系
//        RepositoryParentExtraction repositoryParentExtraction = new RepositoryParentExtraction();
//        Map<String, String> parentMap = repositoryParentExtraction.extractParent(moduleCompilationUnits, classLayerMap);
//
//        // 初始化调用顺序图
//        CallSeqTree callSeqTree = new CallSeqTree();
//
//        // 分析调用链
//        CallSeqAnalyzer analyzer = new CallSeqAnalyzer(callSeqTree, classLayerMap);
//        analyzer.analyzeMethods(classMethodsMap, parentMap);
//
//        // 输出调用图
//        for (Node node : callSeqTree.getNodes()) {
//            System.out.println("节点: " + node.getId() + " (" + node.getType() + ")");
//            for (Edge edge : node.getEdges()) {
//                System.out.println("  -> " + edge.getTarget().getId() + " (weight: " + edge.getWeight() + ")");
//            }
//        }
//
//        String json = callSeqTree.convertToPointTree().toJson();
//        System.out.println(json);
//        List<Node> mergeNodes = callSeqTree.getNodes();
//        String suffixToRemove = "Service:SERVICE";
//
//        // 使用流（Streams）过滤出不需要删除的节点
//        mergeNodes = mergeNodes.stream()
//                .filter(node -> !node.getId().endsWith(suffixToRemove))
//                .collect(Collectors.toList());
//
//        LouvainAlgorithm algorithm = new LouvainAlgorithm(mergeNodes);
//        List<Community> communities = algorithm.detectCommunities();
//        System.out.println("------------------------------");
//        System.out.println(communities.size());
//        for (Community community : communities) {
//            System.out.println("Community: " + community.getMembers().stream().map(n -> n.getId()).collect(Collectors.toList()));
//
//            for (Node node : community.getMembers()) {
//                if(node.getType() == LayerType.CONTROLLER) {
//
//                    for (Map.Entry<Node, Set<Node>> entry : node.dfsWithTwoOrMoreParents(node,community.getParentCountMap()).entrySet()) {
//                        Node key = entry.getKey();
//                        if (entry.getValue().size()>=2)
//                            System.out.println("Key: " + key.getId() + ", Size: " + entry.getValue().size());
//                    }
//                }
//            }
//            System.out.println(""+community.getParentCountMap().size());
//        }
//        EChartsServer.startServer(json);
    }

    public List<Node> getCommunities(String path) throws IOException {
        String projectSrcPath = "D:\\code\\Service-Demo\\testService"; // 修改为项目父模块根路径
        // 设置本地maven仓库路径
        String localMavenRepo = "D:\\maven-reposity";
        // 设置模块源码根路径（包含pom文件的路径）
        String moduleSrcPath = "D:\\code\\Service-Demo\\testService";
        // delombok 模块源码，获取 delomboked 源码路径
        String delombokedModuleSrcPath = getDelombokedSrcPath(moduleSrcPath);
        // 初始化 JavaParser
        JavaParser parser = JavaParserInitializer.initializeParser(projectSrcPath, localMavenRepo, delombokedModuleSrcPath);

        // 解析项目代码
        CodeParser codeParser = new CodeParser(parser);
        List<CompilationUnit> projectCompilationUnits = codeParser.parseProject(projectSrcPath);
        assert delombokedModuleSrcPath != null;
        List<CompilationUnit> moduleCompilationUnits = codeParser.parseProject(delombokedModuleSrcPath);

        System.out.println("项目解析完成，共解析了 " + projectCompilationUnits.size() + " 个文件。");
        System.out.println("模块解析完成，共解析了 " + moduleCompilationUnits.size() + " 个文件。");

        // 识别层
        LayerIdentifier layerIdentifier = new LayerIdentifier();
        Map<String, LayerType> classLayerMap = layerIdentifier.identifyLayers(moduleCompilationUnits);

        // 收集方法
        MethodCollector methodCollector = new MethodCollector();
        Map<String, List<com.github.javaparser.ast.body.MethodDeclaration>> classMethodsMap = methodCollector.collectMethods(moduleCompilationUnits, classLayerMap);

        // 获取继承关系
        RepositoryParentExtraction repositoryParentExtraction = new RepositoryParentExtraction();
        Map<String, String> parentMap = repositoryParentExtraction.extractParent(moduleCompilationUnits, classLayerMap);

        // 初始化调用顺序图
        CallSeqTree callSeqTree = new CallSeqTree();

        // 分析调用链
        CallSeqAnalyzer analyzer = new CallSeqAnalyzer(callSeqTree, classLayerMap);
        analyzer.analyzeMethods(classMethodsMap, parentMap);

        // 输出调用图
        for (Node node : callSeqTree.getNodes()) {
            System.out.println("节点: " + node.getId() + " (" + node.getType() + ")");
            for (Edge edge : node.getEdges()) {
                System.out.println("  -> " + edge.getTarget().getId() + " (weight: " + edge.getWeight() + ")");
            }
        }

        String json = callSeqTree.convertToPointTree().toJson();
        System.out.println(json);
        List<Node> mergeNodes = callSeqTree.getNodes();
        String suffixToRemove = "Service:SERVICE";

        // 使用流（Streams）过滤出不需要删除的节点
        mergeNodes = mergeNodes.stream()
                .filter(node -> !node.getId().endsWith(suffixToRemove))
                .collect(Collectors.toList());

//        LouvainAlgorithm algorithm = new LouvainAlgorithm(mergeNodes);
//        List<Community> communities = algorithm.detectCommunities();
//        System.out.println("------------------------------");
//        System.out.println(communities.size());
//        for (Community community : communities) {
//            System.out.println("Community: " + community.getMembers().stream().map(n -> n.getId()).collect(Collectors.toList()));
//
//            for (Node node : community.getMembers()) {
//                if(node.getType() == LayerType.CONTROLLER) {
//
//                    for (Map.Entry<Node, Set<Node>> entry : node.dfsWithTwoOrMoreParents(node,community.getParentCountMap()).entrySet()) {
//                        Node key = entry.getKey();
//                        if (entry.getValue().size()>=2)
//                            System.out.println("Key: " + key.getId() + ", Size: " + entry.getValue().size());
//                    }
//                }
//            }
//            System.out.println(""+community.getParentCountMap().size());
//        }
        return mergeNodes;
    }

    private static String getDelombokedSrcPath(String moduleSrcPath) {
        // 请设置工作空间位置为本地机器上的某位置
        String workspace = "D:\\code\\workFactory\\";
        String rootPath = moduleSrcPath + "\\src\\main\\java";

        // 如果是在 Windows 系统上，以下的代码不用改动
        // 如果是在 Linux 或 macos 系统上，请将所有的 "\\" 替换为 "/"
        try {
            // 找到 \src 的位置
            int srcIndex = rootPath.indexOf("\\src");
            String moduleName = "";
            String delombokedSrcPath = "";
            // 如果找到了 \src
            if (srcIndex != -1) {
                // 从 \src 前截取路径
                String beforeSrc = rootPath.substring(0, srcIndex);

                // 找到最后一个 \，获取最后的文件夹名称
                int lastSeparator = beforeSrc.lastIndexOf("\\");
                if (lastSeparator != -1) {
                    // 提取模块名称2
                    moduleName = beforeSrc.substring(lastSeparator + 1);
                    System.out.println("提取的模块名称: " + moduleName);
                    delombokedSrcPath = workspace + "delomboked_" + moduleName;
                }
            } else {
                System.out.println("未找到 \\src");
                return null; // 如果未找到 \src，直接返回
            }

            // 构建命令
            // 执行命令 java -jar lombok.jar delombok <源码路径> -d <目标路径>
            // 请确保设置的工作空间位置中有 lombok.jar
            // 请确保 java 命令在系统的环境变量路径中
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(
                    "java", "-jar", "lombok.jar", "delombok",
                    rootPath, "-d", delombokedSrcPath
            );

            // 设置工作目录为工作空间
            builder.directory(new File(workspace));

            // 合并标准输出和错误输出
            builder.redirectErrorStream(true);

            // 启动进程
            Process process = builder.start();

            // 读取进程的输出（防止缓冲区填满导致阻塞）
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
//                    System.out.println(line);
                }
            }

            // 等待进程完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.out.println("Delombok 进程以非零状态码退出: " + exitCode);
                return null;
            }

            // 确保目标目录存在
            Path delombokedPath = Paths.get(delombokedSrcPath);
            if (!Files.exists(delombokedPath)) {
                System.out.println("Delomboked 目录未创建: " + delombokedSrcPath);
                return null;
            }

            // 源文件路径
            Path sourcePath = Paths.get(moduleSrcPath + "\\pom.xml");
            // 目标文件路径
            Path destinationPath = Paths.get(delombokedSrcPath + "\\pom.xml");

            try {
                // 使用 Files.copy() 复制文件
                Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("pom文件复制成功！");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            return delombokedSrcPath;

        } catch (IOException e) {
            System.out.println("IO异常: ");
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.out.println("进程被中断: ");
            e.printStackTrace();
            Thread.currentThread().interrupt(); // 恢复中断状态
        }
        return null;
    }
}
