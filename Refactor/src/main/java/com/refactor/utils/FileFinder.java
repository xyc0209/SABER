package com.refactor.utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @description: Utility class for find specify class
 * @author: xyc
 * @date: 2024-09-26 10:06
 */
public class FileFinder {
    public static List<String> findServiceClasses(String folderPath) {
        List<String> servicePaths = new ArrayList<>();
        File folder = new File(folderPath);
        findServiceClassesRecursively(folder, servicePaths);
        return servicePaths;
    }

    public static void findSvcConClasses(String folderPath, List<CompilationUnit> servicePaths, List<CompilationUnit> controllerPaths) {
        File folder = new File(folderPath);
        findSvcConClassesRecursively(folder, servicePaths, controllerPaths);
    }

    public static void findSvcConPaths(String folderPath, List<String> servicePaths, List<String> controllerPaths) {
        File folder = new File(folderPath);
        findSvcConClassesRecursively2(folder, servicePaths, controllerPaths);
    }


    private static void findServiceClassesRecursively(File folder, List<String> servicePaths) {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                findServiceClassesRecursively(file, servicePaths);
            } else if (file.getName().endsWith(".java")) {
                findServiceClassesInFile(file.getAbsolutePath(), servicePaths);
            }
        }
    }

    private static void findSvcConClassesRecursively(File folder, List<CompilationUnit> servicePaths, List<CompilationUnit> controllerPaths) {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                findSvcConClassesRecursively(file, servicePaths, controllerPaths);
            } else if (file.getName().endsWith(".java")) {
                findSvcConClassesInFile(file.getAbsolutePath(),  servicePaths, controllerPaths);
            }
        }
    }

    private static void findSvcConClassesRecursively2(File folder, List<String> servicePaths, List<String> controllerPaths) {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                findSvcConClassesRecursively2(file, servicePaths, controllerPaths);
            } else if (file.getName().endsWith(".java")) {
                findSvcConClassesInFile2(file.getAbsolutePath(),  servicePaths, controllerPaths);
            }
        }
    }



    public static List<String> findURL(String folderPath) {
        List<String> servicePaths = new ArrayList<>();
        File folder = new File(folderPath);
        findControllerRequestURL(folder, servicePaths);
        return servicePaths;
    }

    public static void findControllerRequestURL(File folder, List<String> servicePaths) {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                findControllerRequestURL(file, servicePaths);
            } else if (file.getName().endsWith(".java")) {
                findRequestURL(file.getAbsolutePath(), servicePaths);
            }
        }
    }

    private static void findServiceClassesInFile(String filePath, List<String> servicePaths) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                    if (n.isAnnotationPresent("Service")) {
                        servicePaths.add(filePath);
                    }
                    super.visit(n, arg);
                }
            }, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void findSvcConClassesInFile(String filePath, List<CompilationUnit> servicePaths, List<CompilationUnit> controllerPaths) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                    if (n.isAnnotationPresent("Service")) {
                        servicePaths.add(cu);
                    }
                    else if (n.isAnnotationPresent("RestController")){
                        controllerPaths.add(cu);
                    }
                    super.visit(n, arg);
                }
            }, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }



    private static void findSvcConClassesInFile2(String filePath, List<String> servicePaths, List<String> controllerPaths) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                    if (n.isAnnotationPresent("Service")) {
                        servicePaths.add(filePath);
                    }
                    else if (n.isAnnotationPresent("RestController")){
                        controllerPaths.add(filePath);
                    }
                    super.visit(n, arg);
                }
            }, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    public static void findController(String filePath, List<CompilationUnit> controllerPaths) throws FileNotFoundException {
        try {
            CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                    super.visit(n, arg);
                    // 检查类是否有 @RestController 注解
                    if (n.isAnnotationPresent("RestController")) {
                        controllerPaths.add(cu);
                    }
                }
            }, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static boolean isController(String filePath) throws FileNotFoundException {

            List<CompilationUnit> controllerPaths = new ArrayList<>();
            CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                    super.visit(n, arg);
                    // 检查类是否有 @RestController 注解
                    if (n.isAnnotationPresent("RestController")) {
                        controllerPaths.add(cu);
                    }
                }
            }, null);
        System.out.println("-------------");
            if (controllerPaths.isEmpty())
                return false;
            else
                return true;
    }
    private static void findRequestURL(String filePath, List<String> servicePaths) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                    super.visit(n, arg);
            // 检查类是否有 @RestController 注解
                    if (n.isAnnotationPresent("RestController")) {
                        // 查找类级别的 @RequestMapping 注解
                        n.getAnnotations().forEach(annotation -> {
                            if (annotation.getNameAsString().equals("RequestMapping")) {
                                // 获取 RequestMapping 注解的所有参数
                                annotation.getChildNodes().forEach(childNode -> {
                                    // 检查每个子节点是否是 StringLiteralExpr
                                    if (childNode instanceof StringLiteralExpr) {
                                        String mappedPath = ((StringLiteralExpr) childNode).getValue();
                                        servicePaths.add(mappedPath);
                                    }
                                });
                            }
                        });
                    }
                }
            }, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getJavaFiles(String servicesDirectory) throws IOException {
        Path start= Paths.get(servicesDirectory);
        List<String> javaFiles;
        int maxDepth = 15;
        Stream<Path> stream = Files.find(start,maxDepth,(filepath, attributes) -> String.valueOf(filepath).endsWith(".java"));
        //ignore .java files in package com.test,but classes outside this package can have "com.test" or "Test" in their names
        javaFiles= stream.sorted().map(String::valueOf)
                .filter(filepath ->
                        (!String.valueOf(filepath).contains("\\com.test\\") && !String.valueOf(filepath).contains("/test/"))
                )
                .collect(Collectors.toList());
        return javaFiles;
    }
    public static String trimPathBeforeSrc(String filePath) {
        int srcIndex = filePath.indexOf("\\src");
        if (srcIndex == -1) {
            srcIndex = filePath.indexOf("/src");
        }

        if (srcIndex != -1) {
            return filePath.substring(0,srcIndex);
        } else {
            return filePath;
        }
    }

    // 获得构建好的jar包路径
    public static List<String> getLatestJarFilePaths(String path) {
        try (Stream<Path> stream = Files.walk(Paths.get(path))) {
            return stream
                    .filter(p -> p.toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(p -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                            return attrs.creationTime().toMillis();
                        } catch (IOException e) {
                            return 0L;
                        }
                    }, Comparator.reverseOrder()))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error finding JAR files: " + e.getMessage());
            return null;
        }
    }

    public static String findCommonURLPrefix(String folderPath) {
        List<String> strings = findURL(folderPath);
        if (strings == null || strings.isEmpty()) {
            return "";  // 处理空列表
        }

        // 选择第一个字符串作为基准
        String firstString = strings.get(0);
        int prefixLength = firstString.length();

        // 遍历列表中的字符串
        for (int i = 1; i < strings.size(); i++) {
            String currentString = strings.get(i);

            // 比较当前字符串与基准字符串
            while (prefixLength > 0 && !currentString.startsWith(firstString.substring(0, prefixLength))) {
                prefixLength--;  // 每次减少前缀长度
            }

            // 如果公共前缀长度为零，提前返回
            if (prefixLength == 0) {
                return "";
            }
        }

        // 返回计算得到的公共前缀
        return firstString.substring(0, prefixLength);
    }

    // 创建目标文件夹
    public static void main(String[] args) {
//        List<String> servicePaths = findServiceClasses("D:\\code\\Service-Demo\\testService1");
////        for (String path : servicePaths) {
////            System.out.println(path);
////        }
//        List<String> serviceModifiedPaths = new ArrayList<>();
//        for(String serviceClassFile: servicePaths){
//            if (ServiceReplaceUtils.serviceReplace(serviceClassFile)){
//                String addServicePath = FileFinder.trimPathBeforeSrc(serviceClassFile);
//                if (!serviceModifiedPaths.contains(addServicePath))
//                    serviceModifiedPaths.add(addServicePath);
//            }
//        }
//        System.out.println(serviceModifiedPaths);
//        List<String> URLList = new ArrayList<>();
//        System.out.println(findCommonURLPrefix("D:\\code\\Service-Demo\\testService1"));


    }

}