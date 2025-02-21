package com.refactor.chain.analyzer.extension;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.refactor.chain.analyzer.layer.LayerType;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * @author: xyc
 * @date: 2024-10-24 17:01
 */
public class RepositoryParentExtraction {
    public  Map<String, String> extractParent(List<CompilationUnit> compilationUnits, Map<String, LayerType> classLayerMap) {
        Map<String,String> parentMap = new HashMap<>();

        for (CompilationUnit cu : compilationUnits) {
            List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
            for (ClassOrInterfaceDeclaration cls : classes) {
                String className = cls.getFullyQualifiedName().orElse(cls.getNameAsString());
                LayerType layer = classLayerMap.getOrDefault(className, LayerType.OTHER);
                System.out.println("extendsName  CLASS NAME" + className);
                System.out.println("LAYER" + layer.toString());
                if (layer == LayerType.REPOSITORY) {
                    // 获取并处理继承的接口
                    cls.getExtendedTypes().forEach(implementedType -> {
                        // 检查是否为参数化类型
                        if (implementedType.isClassOrInterfaceType()) {
                            // 获取类名
                            String extendsName = implementedType.getNameAsString();
                            System.out.println("extendsName" + extendsName);
                            // 获取类型参数
                            System.out.println(implementedType.getTypeArguments().toString());
                            System.out.println("implementedType" + implementedType.toString());
                            if (!extendsName.equals("Repository")) {
                                List<Type> typeArguments = implementedType.getTypeArguments().get();
                                if (!typeArguments.isEmpty() && typeArguments.get(0).isClassOrInterfaceType()) {
                                    ClassOrInterfaceType firstTypeArgument = typeArguments.get(0).asClassOrInterfaceType();
                                    String simpleName = String.valueOf(firstTypeArgument.getName());
                                    String fullyQualifiedName = getValueByKeyContains(classLayerMap, simpleName);
                                    parentMap.put(className, fullyQualifiedName);
                                }
                            }
                        }
                    });
                } else if (layer == LayerType.SERVICE) {
                    cls.getExtendedTypes().forEach(implementedType -> {
                        if (implementedType.isClassOrInterfaceType()) {
                            // 获取类名
                            String extendsName = implementedType.getNameAsString();
                            System.out.println("extendsName" + extendsName);
                            if (implementedType.getTypeArguments().isPresent()) {
                                List<Type> typeArguments = implementedType.getTypeArguments().get();
                                if (!typeArguments.isEmpty()) {
                                    ClassOrInterfaceType typeAargument = typeArguments.get(0).asClassOrInterfaceType();
                                    String simpleName = String.valueOf(typeAargument.getName());
                                    String fullyQualifiedName = getValueByKeyContains(classLayerMap, simpleName);
                                    parentMap.put(className, fullyQualifiedName);
                                }
                            }
                        }
                    });
                }
            }
        }

        return parentMap;
    }

    // 查找包含指定字符串的键，并返回对应的值
    public static String getValueByKeyContains(Map<String, LayerType> map, String searchString) {
        for (Map.Entry<String, LayerType> entry : map.entrySet()) {
            if (entry.getKey().endsWith(searchString)) {
                return entry.getKey(); // 返回对应的值
            }
        }
        return null; // 如果没有找到，返回 null
    }
}