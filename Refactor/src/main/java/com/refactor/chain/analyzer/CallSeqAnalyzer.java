package com.refactor.chain.analyzer;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.refactor.chain.analyzer.layer.LayerType;
import com.refactor.chain.utils.CallSeqTree;


import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CallSeqAnalyzer {
    private final CallSeqTree callSeqTree;
    private final Map<String, LayerType> classLayerMap;


    public CallSeqAnalyzer(CallSeqTree callSeqTree, Map<String, LayerType> classLayerMap) {
        this.callSeqTree = callSeqTree;
        this.classLayerMap = classLayerMap;
    }

    public void analyzeMethods(Map<String, List<MethodDeclaration>> classMethodsMap,Map<String,String> parentMap){
        for (String key: parentMap.keySet()){
            String value = parentMap.get(key);
            System.out.println("parentMap"+parentMap.toString());
            System.out.println("key" +key);
            if (parentMap.get(key) != null) {
                if ((key.toLowerCase().endsWith("service") || key.toLowerCase().endsWith("serviceimpl")) && parentMap.get(key).toLowerCase().endsWith("mapper")) {
                    callSeqTree.addNode(key + ":SERVICE", LayerType.SERVICE);
                    callSeqTree.addNode(value + ":MAPPER", LayerType.SERVICE);
                    callSeqTree.addEdge(key + ":SERVICE", value + ":MAPPER", LayerType.SERVICE);
                }
            }
        }
        System.out.println("classMethodsMap.entrySet()" + classMethodsMap.entrySet().size());
        System.out.println("classMethodsMap-----nnn" +classMethodsMap.get("com.ctrip.framework.apollo.portal.controller.AppController"));
        int k = 0;
        for (Map.Entry<String, List<MethodDeclaration>> entry : classMethodsMap.entrySet()) {
            k++;

            String className = entry.getKey();
            System.out.println("Checking className: " + className);
            if (className.endsWith("AppController"))
                System.out.println("SSSSSSSSSSSSS" + className);
            System.out.println("className:TEST" + className);

        }
        System.out.println("i::::"+k);
        System.out.println("classMethodsMap.entrySet()"+classMethodsMap.entrySet().size());
        int i = 0;
        for (Map.Entry<String, List<MethodDeclaration>> entry : classMethodsMap.entrySet()) {
            i++;

            String className = entry.getKey();
            if (className.endsWith("AppController"))
                System.out.println("SSSSSSSSSSSSS" + className);
            System.out.println("className:TEST" +className);
            LayerType layer = classLayerMap.getOrDefault(className, LayerType.OTHER);
            System.out.println("layer" +layer.toString());
            List<MethodDeclaration> methods = entry.getValue();
            System.out.println("CLASS NAME" + className);
            System.out.println("METHOD" + methods.toString());
            if (classLayerMap.get(className) == LayerType.REPOSITORY) {
                if(methods.isEmpty()){
                    if (parentMap.containsKey(className)){
                        String callerId = className + ":" + layer.toString();
                        String parentName = parentMap.get(className);
                        LayerType typeLayer = classLayerMap.getOrDefault(parentName, LayerType.OTHER);
                        String targetId = parentName+ ":" + typeLayer.toString();
                        if (LayerType.isValidLayerType(layer) && LayerType.isValidLayerType(typeLayer)) {
                            callSeqTree.addNode(callerId, layer);
                            callSeqTree.addNode(targetId, typeLayer);
                            callSeqTree.addEdge(callerId, targetId, layer);
                        }
                    }
                }
                for (MethodDeclaration method : methods) {
                    Type returnType = method.getType();
                    if (returnType.isClassOrInterfaceType()) {
                        ClassOrInterfaceType classType = returnType.asClassOrInterfaceType();
                        if (classType.getTypeArguments().isPresent()) {
                            NodeList<Type> typeArguments = classType.getTypeArguments().get();
                            // 处理泛型参数
                            for (Type typeArgument : typeArguments) {
                                if (typeArgument.isClassOrInterfaceType()) {
                                    ClassOrInterfaceType type = typeArgument.asClassOrInterfaceType();
                                    try {
                                        type.resolve();
                                    }
                                    catch (Exception e) {
                                        // 处理类型解析失败的情况
//                    System.err.println("无法解析 orderRepository 的类型: " + e.getMessage());
                                        System.out.println("无法解析类型: " + e.getMessage());
                                        break;
                                    }
                                    ResolvedReferenceTypeDeclaration resolvedType = type.resolve().asReferenceType().getTypeDeclaration().get();
                                    String qualifiedName = resolvedType.getQualifiedName();
                                    LayerType typeLayer = classLayerMap.getOrDefault(qualifiedName, LayerType.OTHER);
//                                    if (isValidLayerTransition(layer, typeLayer)) {
                                    if (classLayerMap.containsKey(qualifiedName) && LayerType.isValidLayerType(layer) && LayerType.isValidLayerType(typeLayer)) {
                                        String callerId = className + ":" + layer.toString();
                                        String calleeId = qualifiedName + ":" + typeLayer.toString();
                                        callSeqTree.addNode(callerId, layer);
                                        callSeqTree.addNode(calleeId, typeLayer);
                                        callSeqTree.addEdge(callerId, calleeId, layer);
//                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 处理 MyBatis Mapper 类的方法参数
            if (classLayerMap.get(className) == LayerType.MAPPER) {
                for (MethodDeclaration method : methods) {
                    NodeList<Parameter> parameters = method.getParameters();
                    System.out.println("method--" +method.getNameAsString());
                    for (Parameter parameter : parameters) {
                        Type paramType = parameter.getType();
                        if (paramType.isClassOrInterfaceType()) {
                            ClassOrInterfaceType classType = paramType.asClassOrInterfaceType();
                            try {
                                ResolvedReferenceTypeDeclaration resolvedType = classType.resolve().asReferenceType().getTypeDeclaration().get();
                                String qualifiedName = resolvedType.getQualifiedName();
                                System.out.println("qualifiedName" +qualifiedName);
                                LayerType paramLayer = classLayerMap.getOrDefault(qualifiedName, LayerType.OTHER);

                                if (classLayerMap.containsKey(qualifiedName) && LayerType.isValidLayerType(layer) && LayerType.isValidLayerType(paramLayer)) {

                                    String callerId = className + ":" + layer.toString();
                                    String calleeId = qualifiedName + ":" + paramLayer.toString();
                                    System.out.println("callerId" +callerId);
                                    System.out.println("calleeId" +calleeId);
                                    callSeqTree.addNode(callerId, layer);
                                    callSeqTree.addNode(calleeId, paramLayer);
                                    callSeqTree.addEdge(callerId, calleeId, layer);
                                }
                            } catch (Exception e) {
                                System.out.println("无法解析方法参数类型: " + paramType + " in method " + method.getNameAsString() + " of class " + className + " 报错信息为：" + e.getMessage());
                            }
                        }
                    }
                }
            }

            // 处理 使用MyBatis Plus 的service类
            // such as ：
            // @Service
            //public class UserService extends ServiceImpl<UserMapper, User> implements IUserService {
//            if (classLayerMap.get(className) == LayerType.SERVICE) {
//                for (MethodDeclaration method : methods) {
//                    NodeList<Parameter> parameters = method.getParameters();
//                    System.out.println("method--" +method.getNameAsString());
//                    for (Parameter parameter : parameters) {
//                        Type paramType = parameter.getType();
//                        if (paramType.isClassOrInterfaceType()) {
//                            ClassOrInterfaceType classType = paramType.asClassOrInterfaceType();
//                            try {
//                                ResolvedReferenceTypeDeclaration resolvedType = classType.resolve().asReferenceType().getTypeDeclaration().get();
//                                String qualifiedName = resolvedType.getQualifiedName();
//                                System.out.println("qualifiedName" +qualifiedName);
//                                LayerType paramLayer = classLayerMap.getOrDefault(qualifiedName, LayerType.OTHER);
//
//                                if (classLayerMap.containsKey(qualifiedName) && LayerType.isValidLayerType(layer) && LayerType.isValidLayerType(paramLayer)) {
//
//                                    String callerId = className + ":" + layer.toString();
//                                    String calleeId = qualifiedName + ":" + paramLayer.toString();
//                                    System.out.println("callerId" +callerId);
//                                    System.out.println("calleeId" +calleeId);
//                                    callSeqTree.addNode(callerId, layer);
//                                    callSeqTree.addNode(calleeId, paramLayer);
//                                    callSeqTree.addEdge(callerId, calleeId, layer);
//                                }
//                            } catch (Exception e) {
//                                System.out.println("无法解析方法参数类型: " + paramType + " in method " + method.getNameAsString() + " of class " + className + " 报错信息为：" + e.getMessage());
//                            }
//                        }
//                    }
//                }
//            }

            for (MethodDeclaration method : methods) {
                System.out.println("method:"+method.getNameAsString());
                method.accept(new MethodCallVisitor(className, layer, classLayerMap), callSeqTree);
            }
            //针对 无任何调用的情况，添加controller类
            String callerId = className + ":" + layer.toString();
            System.out.println("callerId" +callerId);
            if (callSeqTree.getNode(callerId) == null && LayerType.isValidLayerType(layer) ) {
                callSeqTree.addNode(callerId, layer);
            }
        }
        System.out.println("i::::"+i);
    }

    private class MethodCallVisitor extends VoidVisitorAdapter<CallSeqTree> {
        private final String currentClassName;
        private final LayerType currentLayer;
        private final Map<String, LayerType> classLayerMap;

        public MethodCallVisitor(String className, LayerType layerType, Map<String, LayerType> classLayerMap) {
            this.currentClassName = className;
            this.currentLayer = layerType;
            this.classLayerMap = classLayerMap;
        }

        @Override
        public void visit(MethodCallExpr methodCall, CallSeqTree callSeqTree) {
//            super.visit(methodCall, callSeqTree);
            System.out.println("methodCall"+methodCall.getScope().isPresent());
            if (!methodCall.getScope().isPresent())
                return;
            Optional<Expression> scope = methodCall.getScope();
            NodeList<Expression> arguments = methodCall.getArguments();
            String name = scope.get().toString();

            if (scope.isPresent()) {
                Expression scopeExpr = scope.get();
                try {
                    System.out.println("scopeExpr:"+scopeExpr);
                    ResolvedType scopeType = scopeExpr.calculateResolvedType();
                    System.out.println("scopeType。TO" + scopeType.toString());
                    String calledClassName = scopeType.describe();
                    System.out.println("orderRepository 属于的类: " + calledClassName);
                    LayerType calledLayer = classLayerMap.getOrDefault(calledClassName, LayerType.OTHER);

                    // 记录符合层次调用关系的调用
//                    if (isValidLayerTransition(currentLayer, calledLayer)) {
                    if (classLayerMap.containsKey(calledClassName)) {
                        String callerId = currentClassName + ":" + currentLayer.toString();
                        String calleeId = calledClassName + ":" + calledLayer.toString();

                        if (LayerType.isValidLayerType(currentLayer) && LayerType.isValidLayerType(calledLayer)) {
                            // 添加边到调用顺序图
                            callSeqTree.addNode(callerId, currentLayer);
                            callSeqTree.addNode(calleeId, calledLayer);
                            callSeqTree.addEdge(callerId, calleeId, currentLayer);
                            System.out.println("CALLERiD" +callerId + calleeId);
                        }
//                    }
                    }
                } catch (Exception e) {
                    // 处理类型解析失败的情况
                    //针对mybatisplus无法获取的情况
//                    String target = scopeExpr.toString().toLowerCase();
//                    if (target.endsWith("mapper")){
//                        int lastDotIndex = target.lastIndexOf(".");
//
//                        // 找到倒数第二个"."的位置
//                        int secondLastDotIndex = target.lastIndexOf(".", lastDotIndex - 1);
//                        String calledId = target.substring(0, secondLastDotIndex) + ".mapper" +
//                        callSeqTree.addNode(currentClassName + ":" + currentLayer.toString(), currentLayer);
//                        callSeqTree.addNode(calleeId, calledLayer);
//                        callSeqTree.addEdge(callerId, calleeId, currentLayer);
//
//                    }
//                    System.err.println("无法解析 orderRepository 的类型: " + e.getMessage());
                    System.out.println("无法解析方法调用: " + methodCall + " in class " + currentClassName + " 报错信息为：" + e.getMessage());
                }
            }
            if (!arguments.isEmpty()) {
                for (Expression argument : arguments) {
                    if (argument instanceof MethodCallExpr) {
                        Optional<Expression> argScope = ((MethodCallExpr) argument).getScope();
                        if (argScope.isPresent()) {
                            Expression argScopeExpr = argScope.get();
                            if (argScopeExpr instanceof MethodCallExpr) {
                                argScopeExpr.accept(this, callSeqTree);
                                String fullName = null;
                                try {
//                                    ResolvedMethodDeclaration resolvedMethod = ((MethodCallExpr) argScope.get()).resolve();
                                    ResolvedType scopeType = argScopeExpr.calculateResolvedType();

                                    // 获取返回类型
                                    fullName = scopeType.describe();
                                } catch (Exception e) {
                                    System.out.println("无法解析方法调用: " + methodCall  + "  报错信息为：" + e.getMessage());
                                    return;
//                                    e.printStackTrace();
                                }
                                if (fullName != null) {
                                    if (classLayerMap.containsKey(fullName)) {
                                        argument.accept(this, callSeqTree);
                                    }
                                }

                            }
                            System.out.println("argScope.get().calculateResolvedType().describe()" +argScope.get());
                            try {
                                argScope.get().calculateResolvedType();
                            }
                            catch (Exception e) {
                                System.out.println("无法解析类型: " + argScope.get()  + "  报错信息为：" + e.getMessage());
                                return;
                            }
                            if (classLayerMap.containsKey(argScope.get().calculateResolvedType().describe())) {
                                argument.accept(this, callSeqTree);
                            }
                        }
//                        argument.accept(this, callSeqTree);
                    }
                }
            }

        }
    }



}
