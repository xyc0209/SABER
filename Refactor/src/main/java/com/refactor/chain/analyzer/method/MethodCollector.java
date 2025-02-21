package com.refactor.chain.analyzer.method;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.refactor.chain.analyzer.layer.LayerType;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodCollector {

    public Map<String, List<MethodDeclaration>> collectMethods(List<CompilationUnit> compilationUnits, Map<String, LayerType> classLayerMap) {
        Map<String, List<MethodDeclaration>> classMethodsMap = new HashMap<>();

        for (CompilationUnit cu : compilationUnits) {
            List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
            for (ClassOrInterfaceDeclaration cls : classes) {
                String className = cls.getFullyQualifiedName().orElse(cls.getNameAsString());
                LayerType layer = classLayerMap.getOrDefault(className, LayerType.OTHER);

                // 仅收集目标层的类
                if (layer == LayerType.CONTROLLER || layer == LayerType.SERVICE || layer == LayerType.REPOSITORY) {
                List<MethodDeclaration> methods = cls.getMethods();
                classMethodsMap.put(className, methods);
                }
            }
        }

        return classMethodsMap;
    }
}