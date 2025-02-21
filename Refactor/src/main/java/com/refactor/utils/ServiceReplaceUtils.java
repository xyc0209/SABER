package com.refactor.utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @description: replace service name
 * @author: xyc
 * @date: 2024-09-25 19:04
 */
public class ServiceReplaceUtils {
    private static String TARGET_VALUE = "ts-verification-code-service";
    private  static String REPLACEMENT_VALUE = "Replace-service";
//    private static final Pattern SERVICE_PATTERN = Pattern.compile("\\b(service|Service|SERVICE)\\b$");
    private  static Pattern URL_PATTERN;
    private static boolean modified = false;

    public ServiceReplaceUtils(String TARGET_VALUE, String REPLACEMENT_VALUE, Pattern URL_PATTERN) {
        this.TARGET_VALUE = TARGET_VALUE;
        this.REPLACEMENT_VALUE = REPLACEMENT_VALUE;
        this.URL_PATTERN = URL_PATTERN;
    }

    public static void main(String[] args) {
//        String path = "D:\\code\\Service-Demo\\testService1\\src\\main\\java\\org\\example\\service\\testServiceImpl.java";
        File javaFile = new File("D:\\code\\Service-Demo\\testService1\\src\\main\\java\\org\\example\\service\\testServiceImpl.java");
        try {
            CompilationUnit cu = StaticJavaParser.parse(javaFile);
            HashSet<String>  svcModified = new HashSet<>();
            cu.accept(new MethodVisitor(), svcModified);
            System.out.println(cu);
//            PrintWriter writer = new PrintWriter("D:\\code\\Service-Demo\\testService1\\src\\main\\java\\org\\example\\service\\testServiceImpl.java");
//            writer.println(cu.toString());
            writeModifiedFile(javaFile,cu.toString());
            if (!svcModified.isEmpty())
                System.out.println("-----------"+svcModified.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }



    public static boolean serviceReplace(String filePath){
        File javaFile = new File(filePath);
        try {
            CompilationUnit cu = StaticJavaParser.parse(javaFile);
            HashSet<String>  svcModified = new HashSet<>();
            cu.accept(new MethodVisitor(), svcModified);
//            cu.accept(new MethodVisitor(), new HashSet<>());
            System.out.println(cu);
//            PrintWriter writer = new PrintWriter("D:\\code\\Service-Demo\\testService1\\src\\main\\java\\org\\example\\service\\testServiceImpl.java");
//            writer.println(cu.toString());
            writeModifiedFile(javaFile,cu.toString());
            if (!svcModified.isEmpty())
                return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }
    private static class MethodVisitor extends VoidVisitorAdapter<HashSet<String>> {
        @Override
        public void visit(MethodDeclaration md, HashSet<String> svcModified) {
            super.visit(md, svcModified);
            String svcExisted = null;
            // 遍历方法中的局部变量

//            md.getBody().ifPresent(body -> {
//                body.findAll(VariableDeclarationExpr.class).forEach(vde -> {
//                    for (VariableDeclarator vd : vde.getVariables()) {
//                        String varName = vd.getNameAsString();
//                        Expression initializer = vd.getInitializer().orElse(null);
//                        if (initializer != null && initializer.isStringLiteralExpr()) {
//                            String varValue = initializer.asStringLiteralExpr().getValue();
//                            arg.put(varName, varValue);
//                            // 检查变量值是否为目标值
//                            if (varValue.contains(TARGET_VALUE)) {
//                                // 将目标值替换为新值
//                                initializer.replace(StaticJavaParser.parseExpression("\"" + TARGET_SERVICE + "\""));
//                            }
//                        }
//                    }
//                });
//            });

//            for (Map.Entry<String, Object> entry : arg.entrySet()) {
//                String valueStr = (String) entry.getValue();
//                System.out.println("valueStr"+valueStr);
//                if(isServiceRelated(valueStr) && valueStr.equals(TARGET_VALUE)){
//                    svcExisted = (String) entry.getValue();
//                }
//            }
//            System.out.println("svcExisted"+svcExisted);

            AtomicBoolean apiExisted = new AtomicBoolean(false);
            List<String> svcList = new ArrayList<>();
            // 遍历方法中的方法调用
            md.findAll(MethodCallExpr.class).forEach(methodCall -> {
                String methodName = methodCall.getNameAsString();
                System.out.println("methodName"+methodName);
                for (Expression arg0 : methodCall.getArguments()) {
                    String argValue = getValueFromExpression(arg0);
                    System.out.println("argValue"+argValue);
                    if (isURLMatched(md)) {
                        apiExisted.set(true);
                    }
                    if (argValue.startsWith("\"") && argValue.endsWith("\"")) {
                        argValue = argValue.substring(1, argValue.length() - 1);
                    }
                    if (isServiceRelated(argValue)) {
                        svcList.add(argValue);
                    }
                }
            });
            if (svcList.contains(TARGET_VALUE) && apiExisted.get()){ //serviceName 以方法调用中的实参的形式呈现
                svcModified.add(TARGET_VALUE);
                //替换argValue所在位置为指定值
                md.findAll(MethodCallExpr.class).forEach(methodCall -> {
                    String methodName = methodCall.getNameAsString();
                    List<Expression> arguments = methodCall.getArguments();
                    for (int i = 0; i < arguments.size(); i++) {
                        Expression arg0 = arguments.get(i);
                        String argValue = getValueFromExpression(arg0);
                        if (argValue.contains(TARGET_VALUE)) {
                           //替换arg0的value为指定的值
                            System.out.println("END ++++"+argValue);
                            methodCall.setArgument(i, new StringLiteralExpr(REPLACEMENT_VALUE));
                        }
                    }
                    System.out.println("Method arguments after replacement:");
                    for (Expression arg1 : methodCall.getArguments()) {
                        System.out.println(arg1);
                    }
                });
            } else if (apiExisted.get()) {   //serviceName 以局部变量值的形式呈现
                md.getBody().ifPresent(body -> {
                    body.findAll(VariableDeclarationExpr.class).forEach(vde -> {
                        for (VariableDeclarator vd : vde.getVariables()) {
                            String varName = vd.getNameAsString();
                            Expression initializer = vd.getInitializer().orElse(null);
                            if (initializer != null && initializer.isStringLiteralExpr()) {
                                String varValue = initializer.asStringLiteralExpr().getValue();
//                                arg.put(varName, varValue);
                                // 检查变量值是否为目标值
                                if (varValue.contains(TARGET_VALUE)) {
                                    svcModified.add(TARGET_VALUE);
                                    // 将目标值替换为新值
                                    initializer.replace(StaticJavaParser.parseExpression("\"" + REPLACEMENT_VALUE + "\""));
                                }
                            }
                        }
                    });
                });
            }
        }

        private String getValueFromExpression(Expression expr) {
            if (expr instanceof VariableDeclarationExpr) {
                VariableDeclarationExpr vde = (VariableDeclarationExpr) expr;
                String varName = vde.getVariable(0).getNameAsString();
                return varName;
            } else {
                return expr.toString();
            }
        }


        private boolean isServiceRelated(String value) {
            return value.toLowerCase().endsWith("service");
        }

        private boolean isURLMatched(MethodDeclaration md) {
            Set<Expression> urlExpressions = new HashSet<>();

            // 遍历方法中的所有方法调用
            md.findAll(MethodCallExpr.class).forEach(methodCall -> {
                // 找到与 RestTemplate 相关的方法调用
                if (methodCall.getScope().isPresent() && methodCall.getScope().get().toString().equals("restTemplate")) {
                    // 将方法调用的参数添加到 urlExpressions 集合中
                    urlExpressions.addAll(methodCall.getArguments());
                }
            });

            for (Expression expr : urlExpressions) {
                String url = getValueFromExpression(expr);
                System.out.println("========url======="+url);
                // 针对nano service直接替换对nanoService的调用为normalService的情况
                if (URL_PATTERN ==null)
                    return true;
                // 检查 URL 是否匹配
                Matcher urlMatcher = URL_PATTERN.matcher(url);
                if (urlMatcher.find()) {
                   return true;
                }
            }
            return false;
        }

    }
    private static void writeModifiedFile(File javaFile, String modifiedContent) {
        System.out.println(javaFile.toString());
        FileWriter writer = null;
        try {
            writer = new FileWriter(javaFile);
            writer.write(modifiedContent);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Map<String, String> getSVCModifiedDetails(String Name, String Port){
        Map<String, String> details = new HashMap<>();
        details.put("serviceName", Name);
        details.put("servicePort", Port);
        return details;
    }

    public static Map<String, String> setPath(Map<String, String> svcMap, String path){
        svcMap.put("rulePath", path);

        return svcMap;
    }
}