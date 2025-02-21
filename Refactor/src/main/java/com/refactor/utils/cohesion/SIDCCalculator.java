package com.refactor.utils.cohesion;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.Type;

import java.io.File;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/**
 * @description:
 * @author: xyc
 * @date: 2024-11-12 17:09
 */
public class SIDCCalculator {

    public static void main(String[] args) throws Exception {
        // 指定 Spring 服务的源代码目录
        File sourceFile = new File("D:\\code\\train-ticket-test-main\\train-ticket-test\\train-ticket-test\\ts-config-service\\src\\main\\java\\config\\controller\\ConfigController.java");

        JavaParser javaParser = new JavaParser();
        ParseResult<CompilationUnit> parseResult = javaParser.parse(sourceFile);

        // 提取类中的方法
        List<MethodDeclaration> methods = new ArrayList<>();
        parseResult.getResult().get().findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
            clazz.getMethods().forEach(methods::add);
        });

        // 提取参数和返回类型
        List<Set<String>> parameterTypes = new ArrayList<>();
        List<String> returnTypes = new ArrayList<>();

        for (MethodDeclaration method : methods) {
            // 提取参数类型
            Set<String> params = new HashSet<>();
            method.getParameters().forEach(param -> params.add(param.getType().asString()));
            parameterTypes.add(params);

            // 提取返回类型
            Type returnType = method.getType();
            returnTypes.add(returnType.asString());
        }

        // 计算 SIDC(s)
        double sidc = calculateSIDCScore(parameterTypes, returnTypes);
        System.out.println("SIDC(s) = " + sidc);
        // 计算 LoCmessage 指标
        double locMessage = calculateLoCMessage(parameterTypes, returnTypes);
        System.out.println("LoCmessage = " + locMessage);
    }

    /**
     * 计算 LoCmessage
     *
     * @param inputTypes   每个方法的输入参数类型集合
     * @param returnTypes  每个方法的返回类型集合
     * @return LoCmessage 值
     */
    private static double calculateLoCMessage(List<Set<String>> inputTypes, List<String> returnTypes) {
        int methodCount = inputTypes.size();
        if (methodCount < 2) return 1.0; // 如果方法数少于2，则返回完全内聚

        int totalPairs = methodCount * (methodCount - 1) / 2; // 方法对的总数
        double totalSimilarity = 0;

        // 遍历每一对方法，计算相似性
        for (int i = 0; i < methodCount; i++) {
            for (int j = i + 1; j < methodCount; j++) {
                // 计算输入参数的相似性
                double inputSimilarity = calculateSetSimilarity(inputTypes.get(i), inputTypes.get(j));

                // 计算返回类型的相似性
                double returnSimilarity = returnTypes.get(i).equals(returnTypes.get(j)) ? 1.0 : 0.0;

                // 方法对的平均相似性
                totalSimilarity += (inputSimilarity / 2) + (returnSimilarity / 2);
            }
        }

        // LoCmessage 为方法对相似性的平均值
        return totalSimilarity / totalPairs;
    }

    /**
     * 计算两个集合的相似性
     *
     * @param set1 集合1
     * @param set2 集合2
     * @return 相似度（交集大小/并集大小）
     */
    private static double calculateSetSimilarity(Set<String> set1, Set<String> set2) {
        Set<String> intersection = new HashSet<>(set1); // 求交集
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1); // 求并集
        union.addAll(set2);

        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private static double calculateSIDCScore(List<Set<String>> parameterTypes, List<String> returnTypes) {
        int methodCount = parameterTypes.size();
        if (methodCount < 2) {
            return 1.0; // 单方法或无方法的服务，一致性为 1
        }

        int totalPairs = methodCount * (methodCount - 1) / 2;
        int commonParamPairs = 0;
        int commonReturnPairs = 0;

        // 遍历所有方法对
        for (int i = 0; i < methodCount; i++) {
            for (int j = i + 1; j < methodCount; j++) {
                // 检查参数类型的共性
                Set<String> params1 = parameterTypes.get(i);
                Set<String> params2 = parameterTypes.get(j);
                Set<String> intersection = new HashSet<>(params1);
                intersection.retainAll(params2);
                if (!intersection.isEmpty()) {
                    commonParamPairs++;
                }

                // 检查返回类型的共性
                if (returnTypes.get(i).equals(returnTypes.get(j))) {
                    commonReturnPairs++;
                }
            }
        }
        System.out.println("commonParamPairs" +commonParamPairs);
        System.out.println("commonReturnPairs"+commonReturnPairs);
        // 计算 SIDC(s)
        return (double) (commonParamPairs + commonReturnPairs) / (totalPairs * 2);
    }
}