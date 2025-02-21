package com.refactor.utils.cohesion;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.Type;
import com.refactor.utils.FileFinder;

import java.io.File;
import java.sql.SQLOutput;
import java.util.*;

public class CohesionAnalyzer {

    public static List<ParseResult<CompilationUnit>> parseServiceClasses(List<String> servicePaths) {
        JavaParser javaParser = new JavaParser();
        List<ParseResult<CompilationUnit>> parsedResults = new ArrayList<>();

        for (String servicePath : servicePaths) {
            try {
                File serviceFile = new File(servicePath);
                if (serviceFile.exists() && serviceFile.isFile()) {
                    // 解析文件并添加到结果列表
                    parsedResults.add(javaParser.parse(serviceFile));
                } else {
                    System.err.println("File not found or not a valid file: " + servicePath);
                }
            } catch (Exception e) {
                System.err.println("Failed to parse file: " + servicePath + ". Error: " + e.getMessage());
            }
        }

        return parsedResults;
    }
    public static void main(String[] args) throws Exception {
        // 指定项目源码根目录路径
        String projectPath = "D:\\code\\train-ticket-test-main\\train-ticket-test\\train-ticket-test\\ts-weather-service";


        List<CompilationUnit> servicePaths= new ArrayList<>();
        List<CompilationUnit> controllerPaths= new ArrayList<>();
        FileFinder.findSvcConClasses(projectPath, servicePaths, controllerPaths);
        double dataCohesion = 0;
        Map<String, List<String>> serviceRepositories = new HashMap<>(); // key: Service类名, value: Repository成员变量类型
        System.out.println(controllerPaths.size());
//        FileFinder.findController(projectPath, controllerPaths);
        // 提取所有 @Service 类的方法
        Map<String, List<String>> serviceMethods = new HashMap<>(); // key: Service类名, value: 方法名列表
        for (CompilationUnit cu : servicePaths) {
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
                System.out.println("clazz"+clazz);
                Optional<AnnotationExpr> annotation = clazz.getAnnotationByName("Service");
                if (annotation.isPresent()) {
                    List<String> methods = new ArrayList<>();
                    clazz.getMethods().forEach(method -> methods.add(method.getNameAsString()));
                    serviceMethods.put(clazz.getNameAsString(), methods);
                    // 提取 Service 类中所有成员变量的类型，筛选出以 Repository 结尾的成员变量
                    List<String> repositories = new ArrayList<>();
                    clazz.getFields().forEach(field -> {
                        // 获取成员变量类型
                        Type fieldType = field.getElementType();
                        if (fieldType.asString().endsWith("Repository")) {
                            repositories.add(fieldType.asString());
                        }
                    });
                    serviceRepositories.put(clazz.getNameAsString(), repositories);
                }
            });
        }
        for (Map.Entry<String, List<String>> entry : serviceRepositories.entrySet()) {
            if(entry.getValue().size() >= 1)
                dataCohesion += (double) 1 / entry.getValue().size();
            else
                dataCohesion += 1;
        }

        System.out.println("dataCohesion" +dataCohesion);
        // 提取所有 @Controller 类中对 @Service 方法的调用
        Map<String, Map<String, Set<String>>> serviceMethodUsageByController = new HashMap<>(); // key: Controller类名, value: (Service类名 -> 方法调用集合)
        List<MethodDeclaration> controllerMethods = new ArrayList<>();
        double sidc = 0;
        double locMessage = 0;

        for (CompilationUnit cu : controllerPaths) {
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
                Optional<AnnotationExpr> annotation = clazz.getAnnotationByName("RestController");
                if (annotation.isPresent()) {
                    clazz.getMethods().forEach(controllerMethods::add);
                    String controllerName = clazz.getNameAsString();
                    System.out.println("controllerName"+controllerName);
                    clazz.findAll(MethodCallExpr.class).forEach(methodCall -> {
                        String caller = methodCall.getScope().map(Object::toString).orElse("");
                        serviceMethods.forEach((serviceClass, methods) -> {
                            System.out.println("caller:" +caller);
                            System.out.println("methodCall:" +methodCall);
                            if (caller.equals(""))
                                System.out.println("SUCCESS");
                            if (!caller.equals("") && serviceClass.toLowerCase().contains(caller.toLowerCase())) {
                                methods.forEach(method -> {
                                    System.out.println("methodCall" +methodCall);
                                    System.out.println("method"+method);
                                    System.out.println("--"+methodCall.getChildNodes().get(1).toString());
                                    if (methodCall.getChildNodes().get(1).toString().equals(method)) {
                                        System.out.println("++++++");
                                        serviceMethodUsageByController
                                                .computeIfAbsent(controllerName, k -> new HashMap<>())
                                                .computeIfAbsent(serviceClass, k -> new HashSet<>())
                                                .add(method);
                                    }
                                });
                            }
                        });
                    });
                }
            });
            System.out.println("controllerMethods"+controllerMethods.toString());
            // 提取参数和返回类型
            List<Set<String>> parameterTypes = new ArrayList<>();
            List<String> returnTypes = new ArrayList<>();
            for (MethodDeclaration method : controllerMethods) {
                // 提取参数类型
                Set<String> params = new HashSet<>();
                method.getParameters().forEach(param -> params.add(param.getType().asString()));
                parameterTypes.add(params);

                // 提取返回类型
                Type returnType = method.getType();
                returnTypes.add(returnType.asString());
            }

            // 计算 SIDC(s)
            sidc += calculateSIDCScore(parameterTypes, returnTypes);
            System.out.println("SIDC(s) = " + sidc);
            // 计算 LoCmessage 指标
           locMessage += calculateLoCMessage(parameterTypes, returnTypes);
            System.out.println("LoCmessage = " + locMessage);
            controllerMethods.clear();
        }

        double methodCallCohesion = 0;
        // 输出每个 @Service 类在每个 @Controller 类中被调用的不同方法数量
        for (Map.Entry<String, Map<String, Set<String>>> entry : serviceMethodUsageByController.entrySet()) {
            String controllerName = entry.getKey();
            Map<String, Set<String>> serviceUsage = entry.getValue();

            System.out.println("Controller: " + controllerName);
            for (Map.Entry<String, Set<String>> serviceEntry : serviceUsage.entrySet()) {
                String serviceClass = serviceEntry.getKey();
                System.out.println("serviceClass" +serviceClass);
                Set<String> calledMethods = serviceEntry.getValue();
                System.out.println("  Service: " + serviceClass + ", Unique Methods Called: " + calledMethods.size());
                System.out.println("serviceMethods.get(serviceClass).size()" +serviceMethods.get(serviceClass).size());
                methodCallCohesion += (double) calledMethods.size() / serviceMethods.get(serviceClass).size();
                System.out.println("MCC:  "+ methodCallCohesion);
            }
        }
        int controllerCount = controllerPaths.size();
        int serviceCount = servicePaths.size();
        double agvSIDC = sidc/ controllerCount;
        double agvLocMessage= locMessage/ controllerCount;
        double agvmethodCallCohesion= methodCallCohesion/ controllerCount;
        System.out.println("controllerCount" +controllerCount);
        System.out.println("serviceCount" +serviceCount);
        System.out.println("dataCohesion" +dataCohesion);
        double responsibilityCohesion = ((double) 1 / controllerCount + (double)1 / serviceCount) / 2;
        System.out.println("agvSIDC: "+ agvSIDC);
        System.out.println("MC: "+ agvLocMessage);
        System.out.println("MCC: "+ agvmethodCallCohesion);
        System.out.println("responsibilityCohesion" +responsibilityCohesion);
        System.out.println("dataCohesion" + dataCohesion /serviceCount);
        ServiceCohesion serviceCohesion = new ServiceCohesion();
        serviceCohesion.setSIDC(agvSIDC);
        serviceCohesion.setMC(agvLocMessage);
        serviceCohesion.setMethodCallCohesion(agvmethodCallCohesion);
        serviceCohesion.setDataCohesion(dataCohesion /serviceCount);
        serviceCohesion.setResponsibilityCohesion(responsibilityCohesion);
        System.out.println("caculateCohesion" +serviceCohesion.caculateCohesion());
        System.out.println("-------------------------");
    }


    public static Map<String, Double> getServiceCohesion(String servicePath){
        List<CompilationUnit> servicePaths= new ArrayList<>();
        List<CompilationUnit> controllerPaths= new ArrayList<>();
        FileFinder.findSvcConClasses(servicePath, servicePaths, controllerPaths);
        double dataCohesion = 0;
        Map<String, List<String>> serviceRepositories = new HashMap<>(); // key: Service类名, value: Repository成员变量类型
        System.out.println(controllerPaths.size());
//        FileFinder.findController(projectPath, controllerPaths);
        // 提取所有 @Service 类的方法
        Map<String, List<String>> serviceMethods = new HashMap<>(); // key: Service类名, value: 方法名列表
        for (CompilationUnit cu : servicePaths) {
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
                System.out.println("clazz"+clazz);
                Optional<AnnotationExpr> annotation = clazz.getAnnotationByName("Service");
                if (annotation.isPresent()) {
                    List<String> methods = new ArrayList<>();
                    clazz.getMethods().forEach(method ->
                        methods.add(method.getNameAsString()));
                    serviceMethods.put(clazz.getNameAsString(), methods);

                    // 提取 Service 类中所有成员变量的类型，筛选出以 Repository 结尾的成员变量
                    List<String> repositories = new ArrayList<>();
                    clazz.getFields().forEach(field -> {
                        // 获取成员变量类型
                        Type fieldType = field.getElementType();
                        if (fieldType.asString().endsWith("Repository")) {
                            repositories.add(fieldType.asString());
                        }
                    });
                    serviceRepositories.put(clazz.getNameAsString(), repositories);
                }
            });
        }
        for (Map.Entry<String, List<String>> entry : serviceRepositories.entrySet()) {
            if(entry.getValue().size() >= 1)
                dataCohesion += (double) 1 / entry.getValue().size();
            else
                dataCohesion += 1;
        }

        System.out.println("dataCohesion" +dataCohesion);
        // 提取所有 @Controller 类中对 @Service 方法的调用
        Map<String, Map<String, Set<String>>> serviceMethodUsageByController = new HashMap<>(); // key: Controller类名, value: (Service类名 -> 方法调用集合)
        List<MethodDeclaration> controllerMethods = new ArrayList<>();
        double sidc = 0;
        double locMessage = 0;

        for (CompilationUnit cu : controllerPaths) {
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
                Optional<AnnotationExpr> annotation = clazz.getAnnotationByName("RestController");
                if (annotation.isPresent()) {
                    clazz.getMethods().forEach(controllerMethods::add);
                    String controllerName = clazz.getNameAsString();
                    System.out.println("controllerName"+controllerName);
                    clazz.findAll(MethodCallExpr.class).forEach(methodCall -> {
                        String caller = methodCall.getScope().map(Object::toString).orElse("");
                        serviceMethods.forEach((serviceClass, methods) -> {
                            System.out.println("caller:" +caller);
                            System.out.println("methodCall:" +methodCall);
                            if (caller.equals(""))
                                System.out.println("SUCCESS");
                            if (!caller.equals("") && serviceClass.toLowerCase().contains(caller.toLowerCase())) {
                                methods.forEach(method -> {
                                    System.out.println("methodCall" +methodCall);
                                    System.out.println("method"+method);
                                    System.out.println("--"+methodCall.getChildNodes().get(1).toString());
                                    if (methodCall.getChildNodes().get(1).toString().equals(method)) {
                                        System.out.println("++++++");
                                        serviceMethodUsageByController
                                                .computeIfAbsent(controllerName, k -> new HashMap<>())
                                                .computeIfAbsent(serviceClass, k -> new HashSet<>())
                                                .add(method);
                                    }
                                });
                            }
                        });
                    });
                }
            });
            System.out.println("controllerMethods"+controllerMethods.toString());
            // 提取参数和返回类型
            List<Set<String>> parameterTypes = new ArrayList<>();
            List<String> returnTypes = new ArrayList<>();

            for (MethodDeclaration method : controllerMethods) {
                // 提取参数类型
                Set<String> params = new HashSet<>();
                method.getParameters().forEach(param -> params.add(param.getType().asString()));
                parameterTypes.add(params);

                // 提取返回类型
                Type returnType = method.getType();
                returnTypes.add(returnType.asString());
            }

            // 计算 SIDC(s)
            sidc += calculateSIDCScore(parameterTypes, returnTypes);
            System.out.println("SIDC(s) = " + sidc);
            // 计算 LoCmessage 指标
            locMessage += calculateLoCMessage(parameterTypes, returnTypes);
            System.out.println("LoCmessage = " + locMessage);
            controllerMethods.clear();
        }

        double methodCallRate = 0;
        double serviceEntityCount = 0;
        // 输出每个 @Service 类在每个 @Controller 类中被调用的不同方法数量
        for (Map.Entry<String, Map<String, Set<String>>> entry : serviceMethodUsageByController.entrySet()) {
            String controllerName = entry.getKey();
            Map<String, Set<String>> serviceUsage = entry.getValue();

            System.out.println("Controller: " + controllerName);
            for (Map.Entry<String, Set<String>> serviceEntry : serviceUsage.entrySet()) {
                serviceEntityCount++;
                String serviceClass = serviceEntry.getKey();
                System.out.println("serviceClass" +serviceClass);
                Set<String> calledMethods = serviceEntry.getValue();
                System.out.println("  Service: " + serviceClass + ", Unique Methods Called: " + calledMethods.size());
                System.out.println("serviceMethods.get(serviceClass).size()" +serviceMethods.get(serviceClass).size());
                methodCallRate += (double) calledMethods.size() / serviceMethods.get(serviceClass).size();
                System.out.println("MCC:  "+ methodCallRate);
            }
        }
        double controllerCount = controllerPaths.size();
        double serviceCount = servicePaths.size();
        double agvSIDC = sidc/ controllerCount;
        double agvLocMessage= locMessage/ controllerCount;
        double agvmethodCallCohesion= methodCallRate/ serviceEntityCount;
        double responsibilityCohesion = ((double) 1 / controllerCount + (double)1 / serviceCount) / 2;
        System.out.println("agvSIDC: "+ agvSIDC);
        System.out.println("MC: "+ agvLocMessage);
        System.out.println("MCC: "+ agvmethodCallCohesion);
        System.out.println("responsibilityCohesion" +responsibilityCohesion);
        System.out.println("dataCohesion" + dataCohesion /serviceCount);
        Map<String, Double> cohesionMap = new HashMap<>();
        cohesionMap.put("controllerCount", controllerCount);
        //计算方法调用凝度
        cohesionMap.put("methodCallRate", methodCallRate);
        cohesionMap.put("serviceEntityCount", serviceEntityCount);
        //计算责任一致性
        cohesionMap.put("serviceCount", serviceCount);
        //计算数据内聚性
        cohesionMap.put("dataCohesion", dataCohesion);
        //服务接口数据内聚性
        cohesionMap.put("sidc", sidc);
        //计算消息级内聚性
        cohesionMap.put("locMessage", locMessage);
        ServiceCohesion serviceCohesion = new ServiceCohesion();
        serviceCohesion.setSIDC(agvSIDC);
        serviceCohesion.setMC(agvLocMessage);
        serviceCohesion.setMethodCallCohesion(agvmethodCallCohesion);
        serviceCohesion.setDataCohesion(dataCohesion /serviceCount);
        serviceCohesion.setResponsibilityCohesion(responsibilityCohesion);
        cohesionMap.put("cohesion" ,serviceCohesion.caculateCohesion());
        return cohesionMap;
    }


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
        System.out.println("methodCount：" +methodCount);
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
        System.out.println("totalPairs" +totalPairs);
        // 计算 SIDC(s)
        return (double) (commonParamPairs + commonReturnPairs) / (totalPairs * 2);
    }
}
