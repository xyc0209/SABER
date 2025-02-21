package com.refactor.utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;

import java.io.File;
import java.util.*;

/**
 * @description:
 * @author: xyc
 * @date: 2024-11-17 11:54
 */


public class ControllerParser {

    public static void main(String[] args) throws Exception {
        String filePath = "D:\\code\\demo-collection\\Service-Demo\\testService\\src\\main\\java\\org\\example\\controller\\FoodController.java";
        CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
        Set<Map<String, String>> callDetails = new HashSet<>();
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
            // 检查是否是 Controller 类
            if (clazz.isAnnotationPresent("RestController")) {
                // 获取类级别的 URL
                String classUrl = getClassLevelUrl(clazz);

                // 提取类中的 Service 字段及其类型
                HashMap<String, String> serviceFields = getServiceFields(clazz);

                // 遍历每个方法
                clazz.getMethods().forEach(method -> {
                    // 获取方法级别的 URL
                    String methodUrl = getMethodLevelUrl(method);

                    // 拼接完整 URL
                    String fullUrl = classUrl + methodUrl;
                    Map<String, String> callMap = new HashMap<>();
                    System.out.println("Method URL: " + fullUrl);
                    callMap.put("url", fullUrl);
                    // 查找方法体中的 Service 调用
                    if (method.getBody().isPresent()) {
                        method.getBody().get().findAll(MethodCallExpr.class).forEach(call -> {
                            Optional<NameExpr> caller = call.getScope().filter(NameExpr.class::isInstance).map(NameExpr.class::cast);
                            caller.ifPresent(scope -> {
                                String serviceName = scope.getNameAsString();
                                if (serviceFields.containsKey(serviceName)) {
                                    String serviceType = serviceFields.get(serviceName);
                                    System.out.println("Service Object: " + serviceName + " (Type: " + serviceType + ")");
                                    System.out.println("Called Method: " + call.getNameAsString());
                                    callMap.put("serviceObjectName", serviceName);
                                    callMap.put("Type", serviceType);
                                }
                            });
                        });
                    }
                    callDetails.add(callMap);
                });
            }
        });
    }

    public static void getControllerDetails(CompilationUnit cu, Set<Map<String, String>> callDetails){
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
            // 检查是否是 Controller 类
            if (clazz.isAnnotationPresent("RestController")) {
                // 获取类级别的 URL
                String classUrl = getClassLevelUrl(clazz);

                // 提取类中的 Service 字段及其类型
                HashMap<String, String> serviceFields = getServiceFields(clazz);

                // 遍历每个方法
                clazz.getMethods().forEach(method -> {
                    // 获取方法级别的 URL
                    String methodUrl = getMethodLevelUrl(method);

                    // 拼接完整 URL
                    String fullUrl = classUrl + methodUrl;
                    Map<String, String> callMap = new HashMap<>();
                    System.out.println("Method URL: " + fullUrl);
                    callMap.put("url", fullUrl);
                    // 查找方法体中的 Service 调用
                    if (method.getBody().isPresent()) {
                        method.getBody().get().findAll(MethodCallExpr.class).forEach(call -> {
                            Optional<NameExpr> caller = call.getScope().filter(NameExpr.class::isInstance).map(NameExpr.class::cast);
                            caller.ifPresent(scope -> {
                                String serviceName = scope.getNameAsString();
                                if (serviceFields.containsKey(serviceName)) {
                                    String serviceType = serviceFields.get(serviceName);
                                    System.out.println("Service Object: " + serviceName + " (Type: " + serviceType + ")");
                                    System.out.println("Called Method: " + call.getNameAsString());
                                    callMap.put("serviceObjectName", serviceName);
                                    callMap.put("method", call.getNameAsString());
                                    callMap.put("Type", serviceType);
                                }
                            });
                        });
                    }
                    callDetails.add(callMap);
                });
            }
        });
    }

    // 获取类上的 URL
    private static String getClassLevelUrl(ClassOrInterfaceDeclaration clazz) {
        return clazz.getAnnotationByName("RequestMapping")
                .flatMap(annotation -> {
                    if (annotation.isSingleMemberAnnotationExpr()) {
                        // 获取注解的值并检查是否为 StringLiteralExpr 类型
                        return annotation.asSingleMemberAnnotationExpr()
                                .getMemberValue()
                                .toStringLiteralExpr();
                    }
                    return Optional.empty();
                })
                .map(StringLiteralExpr::asString) // 将 StringLiteralExpr 转为 String
                .orElse("");
    }


    // 获取方法上的 URL
    private static String getMethodLevelUrl(MethodDeclaration method) {
        for (AnnotationExpr annotation : method.getAnnotations()) {
            switch (annotation.getNameAsString()) {
                case "GetMapping":
                case "PostMapping":
                case "PutMapping":
                case "DeleteMapping":
                    if (annotation.isSingleMemberAnnotationExpr()) {
                        return annotation.asSingleMemberAnnotationExpr()
                                .getMemberValue() // 获取注解值
                                .toStringLiteralExpr() // 检查是否是 StringLiteralExpr 类型
                                .map(StringLiteralExpr::asString) // 转换为 String
                                .orElse(""); // 如果没有值则返回空字符串
                    }
            }
        }
        return "";
    }


    // 提取 Service 字段及其类型
    private static HashMap<String, String> getServiceFields(ClassOrInterfaceDeclaration clazz) {
        HashMap<String, String> services = new HashMap<>();
        clazz.findAll(VariableDeclarator.class).forEach(var -> {
            if (var.getType().isClassOrInterfaceType()) {
                String type = var.getType().asClassOrInterfaceType().getNameAsString();
                String name = var.getNameAsString();
                services.put(name, type);
            }
        });
        return services;
    }
}
