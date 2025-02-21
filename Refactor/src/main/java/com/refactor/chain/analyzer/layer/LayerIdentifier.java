package com.refactor.chain.analyzer.layer;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LayerIdentifier {

    public Map<String, LayerType> identifyLayers(List<CompilationUnit> compilationUnits) {
        Map<String, LayerType> classLayerMap = new HashMap<>();

        for (CompilationUnit cu : compilationUnits) {
            List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
            for (ClassOrInterfaceDeclaration cls : classes) {
                String className = cls.getFullyQualifiedName().orElse(cls.getNameAsString());
                LayerType layer = determineLayer(cu, cls);
                classLayerMap.put(className, layer);
            }
        }

        return classLayerMap;
    }

    private LayerType determineLayer(CompilationUnit cu, ClassOrInterfaceDeclaration cls) {
        // 优先通过注解识别
        System.out.println("cls.getNameAsString()"+ cls.getNameAsString());
        if (cls.isAnnotationPresent("Controller") || cls.isAnnotationPresent(RestController.class)) {
            return LayerType.CONTROLLER;
        }
        if (cls.isAnnotationPresent("Service")) {
            return LayerType.SERVICE;
        }
        if (cls.isAnnotationPresent("Repository")) {
            return LayerType.REPOSITORY;
        }
        if (cls.isAnnotationPresent("Mapper")) {
            return LayerType.MAPPER;
        }
        if (cls.isAnnotationPresent("Entity")) {
            return LayerType.ENTITY;
        }

        // 如果没有注解，可以通过包名识别
        Optional<String> packageName = cu.getPackageDeclaration().map(pd -> pd.getNameAsString());
        if (packageName.isPresent()) {
            String pkg = packageName.get();
            if (pkg.contains(".controller")) {
                return LayerType.CONTROLLER;
            }
            if (pkg.contains(".service")) {
                return LayerType.SERVICE;
            }
            if (pkg.contains(".repository")) {
                return LayerType.REPOSITORY;
            }
            if (pkg.contains(".mapper")) {
                return LayerType.MAPPER;
            }
            if (pkg.contains(".entity") || pkg.contains(".model") || pkg.contains(".pojo")) {
                return LayerType.ENTITY;
            }
        }

        String name = cls.getNameAsString().toLowerCase();
        if (name.endsWith("controller")) {
            return LayerType.CONTROLLER;
        }
        if (name.endsWith("service")) {
            return LayerType.SERVICE;
        }
        if (name.endsWith("repository")) {
            return LayerType.REPOSITORY;
        }
        if (name.endsWith(".mapper")) {
            return LayerType.MAPPER;
        }
        if (name.endsWith("entity")) {
            return LayerType.ENTITY;
        }

        return LayerType.OTHER;
    }
}
