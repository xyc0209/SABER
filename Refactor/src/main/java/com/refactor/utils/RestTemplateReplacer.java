package com.refactor.utils;

/**
 * @description:
 * @author: xyc
 * @date: 2024-11-14 16:00
 */
import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.*;
import com.refactor.dto.ParseStorage;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class RestTemplateReplacer {

    public static void main(String[] args) throws IOException {
        //1.确定需要合并的nano service，获取nano service的controller url集合
        String servicePath = "D:\\code\\demo-collection\\Service-Demo\\routeservice";
        List<String> serviceModifiedPaths = new ArrayList<>(); //记录更改服务调用源码的的服务路径，后续用于打包
        Map<String, Map<String, String>> serviceModifiedDetails = new HashMap<>();
        File nanoPath = new File(servicePath);
        List<String> apiUrls = new ArrayList<>();
        String path1 = "/api/food";
        String svcName = "foodservice";
        System.out.println("svcName" +svcName);
        String newServiceName = "testservice";
        ServiceReplaceUtils serviceReplaceUtils1 = new ServiceReplaceUtils(svcName, newServiceName, Pattern.compile( ".*(" + java.util.regex.Pattern.quote(path1) + ".*)?"));
        List<String> serviceClassFiles = FileFinder.findServiceClasses(servicePath);
        for(String serviceClassFile: serviceClassFiles){
            if (serviceReplaceUtils1.serviceReplace(serviceClassFile)){
                String addServicePath = FileFinder.trimPathBeforeSrc(serviceClassFile);
                if (!serviceModifiedPaths.contains(addServicePath))
                    serviceModifiedDetails.put(addServicePath, ServiceReplaceUtils.getSVCModifiedDetails(FileFactory.getServiceDetails(addServicePath).get("serviceName"), FileFactory.getServiceDetails(addServicePath).get("servicePort")));
//                        serviceModifiedPaths.add(addServicePath);
            }
        }
        System.out.println("serviceModifiedDetails: " + serviceModifiedDetails.toString());
        //2.适应度函数确定合并到的正常服务
        //3.合并，修改合并后服务内部调用的源码，修改服务外部调用的源码
        String projectPath = "D:\\code\\demo-collection\\Service-Demo\\testService";
        List<String> servicePaths= new ArrayList<>();
        List<String> controllerPaths= new ArrayList<>();
        FileFinder.findSvcConPaths(projectPath, servicePaths, controllerPaths);
        Set<Map<String, String>> callDetails = new HashSet<>();
        // 假设你已经获取了 CompilationUnit（即类文件的解析结果）
//        String filePath = "D:\\code\\demo-collection\\Service-Demo\\testService\\src\\main\\java\\org\\example\\service\\testServiceImpl1.java";
//        CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
        for (String controlPath: controllerPaths){
            CompilationUnit cu = StaticJavaParser.parse(new File(controlPath));
            ControllerParser.getControllerDetails(cu, callDetails);
        }

        for (String filePath : servicePaths) {
            CompilationUnit cu = StaticJavaParser.parse(new File(filePath));
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
                Optional<AnnotationExpr> annotation = clazz.getAnnotationByName("Service");
                if (annotation.isPresent()) {
                    clazz.getMethods().forEach(method -> {
                        method.getBody().ifPresent(body -> {
                            List<Expression> restTemplateCalls = findRestTemplateCalls(body);
                            for (Expression expr : restTemplateCalls) {
                                String callObject = null;
                                String callType= null;
                                String callMethod = null;
                                System.out.println("Found RestTemplate call: " + expr);
                                String fullRawString = expr.toString();
                                int firstIndex = fullRawString.indexOf('/');
                                int lastIndex = fullRawString.lastIndexOf('/');
                                String mappingUrl = null;
                                if (firstIndex != -1 && lastIndex != -1 && firstIndex != lastIndex) {
                                    // 截取子字符串，包括第一个和最后一个 /
                                    mappingUrl = fullRawString.substring(firstIndex, lastIndex + 1);
                                }
                                for (Map<String, String> callMap: callDetails){
                                    if(callMap.get("url").contains(mappingUrl)){
                                        callObject = callMap.get("serviceObjectName");
                                        callMethod = callMap.get("method");
                                        callType = callMap.get("Type");
                                        break;
                                    }
                                }
                                String[] callTypeWrapper = {callType};
                                // 逻辑扩展部分：添加成员变量并替换调用
                                if (callObject != null && callMethod != null) {
                                    // 1. 添加成员变量到类中
                                    boolean fieldExists = clazz.getFields().stream()
                                            .anyMatch(field -> field.getElementType().asString().equals(callTypeWrapper[0]));

                                    if (!fieldExists) {
                                        // 创建新的字段声明
                                        FieldDeclaration newField = new FieldDeclaration();
                                        VariableDeclarator variable = new VariableDeclarator(
                                                StaticJavaParser.parseClassOrInterfaceType(callType),
                                                callObject + "Impl"
                                        );
                                        newField.addVariable(variable);
                                        newField.addModifier(Modifier.Keyword.PRIVATE);
                                        // 添加 @Autowired 注解，不带括号
                                        newField.addAnnotation(new MarkerAnnotationExpr("Autowired"));

                                        // 获取当前类的成员列表
                                        NodeList<BodyDeclaration<?>> members = clazz.getMembers();

                                        // 找到第一个字段的位置
                                        int insertIndex = 0;
                                        for (int i = 0; i < members.size(); i++) {
                                            if (members.get(i) instanceof FieldDeclaration) {
                                                insertIndex = i;
                                                break;
                                            }
                                        }

                                        // 插入新字段到类成员的适当位置
                                        members.add(insertIndex, newField);

                                        System.out.println("Added member variable: " + newField);
                                    }
                                }
                                clazz.getFields().forEach(field -> System.out.println("Field in class: " + field));



                                // 解析并修改 RestTemplate 调用
//                            modifyRestTemplateCall(expr, method);
                                HashMap<String, String> hashMap = new HashMap<>();
                                ParseStorage parseStorage = new ParseStorage();
                                parseStorage.getMethodDeclarations().add(method);
                                MethodCallExpr methodCallExpr = (MethodCallExpr) expr;
                                NameExpr nameExpr = (NameExpr) methodCallExpr.getScope().get();
                                System.out.println("expr.toString()" + nameExpr.getNameAsString());

                                HashMap<String, Integer> serviceName = ParseResultUtils.processMethods(hashMap, parseStorage, nameExpr.getNameAsString());
                                String callService = serviceName.keySet().iterator().next();
                                System.out.println("serviceName" + callService);

                                modifyRestTemplateCall(expr, method, callService, callObject, callMethod);
                            }
                        });
                    });
                }
            });
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                writer.write(cu.toString());
                System.out.println("Saved modified source to: " + filePath);
                System.out.println(cu.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private static List<Expression> findHttpArgs(BlockStmt body) {
        List<Expression> httpEntityArguments = new ArrayList<>();

        // 查找方法体中的所有 HttpEntity 构造函数
        body.findAll(ObjectCreationExpr.class).forEach(objectCreationExpr -> {
            // 检查是否是 HttpEntity 类型的构造函数
            if (objectCreationExpr.getType().getNameAsString().equals("HttpEntity")) {
                // 获取构造函数的参数
                List<Expression> arguments = objectCreationExpr.getArguments();
                // 打印构造函数的参数（可以根据需求进一步处理）
                arguments.forEach(arg -> System.out.println("HttpEntity constructor argument: " + arg));
                httpEntityArguments.addAll(arguments);
            }
        });
        return httpEntityArguments;
    }

    // 查找方法体中的 RestTemplate 调用
    private static List<Expression> findRestTemplateCalls(BlockStmt body) {
        List<Expression> restTemplateCalls = new ArrayList<>();
//        // 查找方法体中的所有 HttpEntity 构造函数
//        body.findAll(ObjectCreationExpr.class).forEach(objectCreationExpr -> {
//            // 检查是否是 HttpEntity 类型的构造函数
//            if (objectCreationExpr.getType().getNameAsString().equals("HttpEntity")) {
//                // 获取构造函数的参数
//                List<Expression> arguments = objectCreationExpr.getArguments();
//                // 打印构造函数的参数（可以根据需求进一步处理）
//                arguments.forEach(arg -> System.out.println("HttpEntity constructor argument: " + arg));
//            }
//        });

        // 查找方法体中的所有 restTemplate 调用
        body.findAll(MethodCallExpr.class).forEach(call -> {
            // 检查调用对象的类型是否为 RestTemplate
            if (call.getScope().isPresent()) {
                Expression scope = call.getScope().get();
                if (scope instanceof NameExpr) {
                    NameExpr nameExpr = (NameExpr) scope;
                    if (nameExpr.getNameAsString().equals("restTemplate")) {
                        // 如果是 RestTemplate 的方法调用，添加到结果列表
                        restTemplateCalls.add(call);
                    }
                }
            }
        });

        return restTemplateCalls;
    }



    // 修改 RestTemplate 调用，替换为 ServiceImpl 的方法调用
    private static void modifyRestTemplateCall(Expression restTemplateCall, MethodDeclaration method, String callService, String callObject, String callMethod) {
        List<Expression> httpEntityArguments = RestTemplateReplacer.findHttpArgs(method.getBody().get());
        System.out.println("httpEntityArguments:"+httpEntityArguments.toString());
        if (restTemplateCall instanceof MethodCallExpr) {
            MethodCallExpr methodCall = (MethodCallExpr) restTemplateCall;

            // 提取 ResponseEntity 泛型类型（假设是 ResponseEntity<HttpEntity>）

            Optional<Type> returnType = extractGenericType(methodCall);
            if (returnType.isPresent()) {
                Type genericType = returnType.get();
                System.out.println("Inferred generic type: " + genericType);

                // 假设替换为 `serviceImpl` 的方法调用，命名规则为原方法名
                String serviceMethodName = callMethod;
                MethodCallExpr serviceMethodCall = new MethodCallExpr(new NameExpr(callObject + "Impl"), serviceMethodName);

                // 提取 HttpMethod 类型：GET 或 POST
                if (methodCall.getArguments().size() >= 3) {
                    Expression httpMethodArg = methodCall.getArguments().get(1); // 第二个参数是 HttpMethod
                    if (httpMethodArg instanceof FieldAccessExpr) {
                        FieldAccessExpr fieldAccessExpr = (FieldAccessExpr) httpMethodArg;
                        String httpMethod = fieldAccessExpr.getNameAsString(); // 获取常量名（GET 或 POST）
                        System.out.println("HttpMethod: " + httpMethod); // 打印 HttpMethod

                        if ("GET".equalsIgnoreCase(httpMethod)) {
                            serviceMethodCall.addArgument(httpEntityArguments.get(0)); // 将 headers 添加到新的方法调用
//                                        System.out.println("headers"+headers);
                            // 对于 GET 请求，传递 headers 作为参数
//                            if (methodCall.getArguments().size() >= 3) {
//                                Expression requestEntity = methodCall.getArguments().get(2); // 第三个参数是 requestEntity
//                                if (requestEntity instanceof ObjectCreationExpr) {
//                                    // 提取 headers 参数
//                                    ObjectCreationExpr entityExpr = (ObjectCreationExpr) requestEntity;
//                                    if (entityExpr.getArguments().size() > 0) {
//                                        Expression headers = entityExpr.getArguments().get(0); // 获取 headers 参数
//                                        serviceMethodCall.addArgument(headers); // 将 headers 添加到新的方法调用
//                                        System.out.println("headers"+headers);
//                                    }
//                                }
//                            }
                        } else if ("POST".equalsIgnoreCase(httpMethod)) {
                            serviceMethodCall.addArgument(httpEntityArguments.get(0)); // 将 headers 添加到新的方法调用
                            serviceMethodCall.addArgument(httpEntityArguments.get(1));
                        }
                    }
                }

                // 输出修改后的方法调用
                System.out.println("Replaced call: " + serviceMethodCall);

                // 将 methodCall 替换为 serviceMethodCall
                restTemplateCall.replace(serviceMethodCall);
                // 修改 ResponseEntity 变量的类型
                String valName = modifyResponseEntityVariableType(method, genericType);
                replaceGetBodyCallsWithData(method, valName);
            }
        }
    }

    // 修改 ResponseEntity 变量的类型
    private static String modifyResponseEntityVariableType(MethodDeclaration method, Type newType) {
        final String[] valName = new String[1];
        // 查找所有的变量声明，找到类型为 ResponseEntity 的声明
        method.getBody().ifPresent(body -> {
            body.findAll(VariableDeclarationExpr.class).forEach(varDecl -> {
                varDecl.getVariables().forEach(variable -> {
                    // 如果是 ResponseEntity 类型的变量
                    if (variable.getType().isClassOrInterfaceType() && variable.getType().asClassOrInterfaceType().getNameAsString().equals("ResponseEntity")) {
                        ClassOrInterfaceType responseEntityType = variable.getType().asClassOrInterfaceType();
                        // 修改泛型类型为新的类型（即从 exchange 中推断出的类型）
                        if (responseEntityType.getTypeArguments().isPresent()) {
//                            responseEntityType.getTypeArguments().get().set(0, newType);
                            variable.setType(newType);
                        }
                        valName[0] = variable.getNameAsString();
                        // 输出修改后的变量声明
                        System.out.println("Modified variable declaration: " + varDecl + "VAL NAME" + valName[0]);
                    }
                });
            });
        });
        return valName[0];
    }

    // 替换 getBody().getData() 调用为 getData()
    private static void replaceGetBodyCallsWithData(MethodDeclaration method, String valName) {
        method.getBody().ifPresent(body -> {
            body.findAll(MethodCallExpr.class).forEach(call -> {
                // 查找所有的 getBody() 调用
                if (call.getNameAsString().equals("getBody")) {
                    // 获取调用的变量名
                    if (call.getScope().isPresent() && call.getScope().get() instanceof NameExpr) {
                        NameExpr scope = (NameExpr) call.getScope().get();
                        String variableName = scope.getNameAsString();
                        System.out.println("Variable being accessed: " + variableName);

                        // 如果变量名匹配与 restTemplate 调用相关的变量 valName（例如 re）
                        if (variableName != null && variableName.equals(valName)) {
                            // 只删除 getBody() 调用，保留后续调用
                            // 替换 getBody() 为当前变量
                            Expression parentScope = call.getScope().get(); // 获取调用 getBody() 的对象

                            // 获取当前的父方法调用（例如 getBody().getData()）
                            if (call.getParentNode().isPresent() && call.getParentNode().get() instanceof MethodCallExpr) {
                                MethodCallExpr parentCall = (MethodCallExpr) call.getParentNode().get();
                                System.out.println(parentCall);
                                // 将 getBody() 删除，保留后续调用
                                parentCall.setScope(parentScope); // 保持原变量名，如 re
                            }
                            else
                                call.replace(scope);
                            // 删除 getBody() 调用
                            call.remove();
                        }
                    }
                }
            });
        });
    }




    // 提取 ResponseEntity 泛型类型（例如 HttpEntity）
    private static Optional<Type> extractGenericType(MethodCallExpr methodCall) {
        if (methodCall.getArguments().size() >= 4) {
            // 获取第4个参数，这里是 new ParameterizedTypeReference<HttpEntity>() {}
            Expression param = methodCall.getArguments().get(3);
            if (param instanceof ObjectCreationExpr) {
                ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr) param;
                // 检查对象创建的类是否为 ParameterizedTypeReference 类型
                if (objectCreationExpr.getType().getNameAsString().equals("ParameterizedTypeReference")) {
                    // 访问泛型类型参数
                    Type type = objectCreationExpr.getType().getTypeArguments().get().get(0); // 获取泛型参数
                    System.out.println("Extracted generic type: " + type);
                    return Optional.of(type);
                }
            }
            else if (param instanceof ClassExpr) {
                System.out.println("+++++++++++");
                ClassExpr classExpr = (ClassExpr) param;
                // 提取类型
                Type type = classExpr.getType();
                System.out.println("Extracted generic type: " + type);
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

}
