package com.refactor.utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.refactor.context.RestTemplateParameterContext;
import com.refactor.context.StringVariableContext;
import com.refactor.context.UrlContext;
import com.refactor.detail.ModificationRecorder;
import com.refactor.detail.model.CodeModification;
import com.refactor.enumeration.ModificationType;
import com.refactor.enumeration.RequestMethod;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Cocoicobird
 * @version 1.0
 */
public class JavaParserUtils {

    private static final String REGEX = "(localhost|((\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d))(:\\d{0,5})";;
    private static final String PORT = "^[1-9][0-9]{0,3}|^0";
    private static final Pattern REGEX_PATTERN = Pattern.compile(REGEX);
    private static final Pattern PORT_PATTERN = Pattern.compile(PORT);

    /**
     * 为 java 文件添加类注解
     * @param javaFile java 文件路径
     * @param annotation 注解
     */
    public static void addAnnotation(String javaFile, String annotation, String importAnnotation) throws IOException {
        CompilationUnit compilationUnit = StaticJavaParser.parse(new File(javaFile));
        compilationUnit.addImport(importAnnotation);
        if (compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).isPresent()) {
            ClassOrInterfaceDeclaration classOrInterfaceDeclaration = compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).get();
            NodeList<AnnotationExpr> annotations = classOrInterfaceDeclaration.getAnnotations();
            for (AnnotationExpr annotationExpr : annotations) {
                if (annotationExpr.toString().equals(annotation)) {
                    return;
                }
            }
            classOrInterfaceDeclaration.addAnnotation(StaticJavaParser.parseAnnotation(annotation));
            Files.write(new File(javaFile).toPath(), compilationUnit.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    public static boolean containsEndPointBasedInteraction(String javaFile) throws FileNotFoundException {
        CompilationUnit compilationUnit = StaticJavaParser.parse(new File(javaFile));
        List<Object> flag = new LinkedList<>();
        new StringLiteralExprVisitor().visit(compilationUnit, flag);
        return !flag.isEmpty();
    }

    private static class StringLiteralExprVisitor extends VoidVisitorAdapter<List<Object>> {
        @Override
        public void visit(StringLiteralExpr n, List<Object> arg) {
            Matcher matcher = REGEX_PATTERN.matcher(n.getValue());
            if (matcher.find()) {
                arg.add(n);
            }
        }
    }

    /**
     * 判断是否为启动类
     * @param javaFile java 文件路径
     */
    public static boolean isStartupClass(String javaFile) throws FileNotFoundException {
        CompilationUnit compilationUnit = StaticJavaParser.parse(new File(javaFile));
        List<Object> flag = new LinkedList<>();
        new StartupClassVisitor().visit(compilationUnit, flag);
        return !flag.isEmpty();
    }

    /**
     * 单个 java 文件的 API 解析器 这里针对的是单个控制器类的解析 这里信息更全面
     * @param javaFile java 文件路径 一般是 Controller 类
     */
    public static UrlContext singleJavaApiParser(String javaFile) throws IOException {
        CompilationUnit compilationUnit = StaticJavaParser.parse(new File(javaFile));
        UrlContext urlItem = new UrlContext();
        urlItem.setFullQualifiedName(getPackageName(new File(javaFile)));
        new ClassAnnotationVisitor().visit(compilationUnit, urlItem);
        new MethodAnnotationVisitor().visit(compilationUnit, urlItem);
        return urlItem;
    }

    /**
     * 获取一个 java 文件中的所有方法与 URL 的映射
     * @param javaFile java 文件
     * @return 方法与 URL 的映射
     */
    public static Map<String, String> getMethodToApi(File javaFile) throws FileNotFoundException {
        Map<String, String> methodToApi = new HashMap<>();
        CompilationUnit compilationUnit = StaticJavaParser.parse(javaFile);
        UrlContext urlItem = new UrlContext();
        urlItem.setFullQualifiedName(getPackageName(javaFile));
        new ClassAnnotationVisitor().visit(compilationUnit, urlItem);
        new MethodAnnotationVisitor().visit(compilationUnit, urlItem);
        String preUrl = "";
        if (urlItem.getUrl1() != null) {
            preUrl = urlItem.getUrl1().substring(1, urlItem.getUrl1().length() - 1);
            if (!preUrl.startsWith("/")) {
                preUrl = "/" + preUrl;
            }
        }
        for (String methodName : urlItem.getUrl2().keySet()) {
            String sufUrl = urlItem.getUrl2().get(methodName);
            if ("\"\"".equals(sufUrl) && "".equals(urlItem.getHttpMethod().get(methodName))) {
                methodToApi.put(methodName, preUrl);
                continue;
            }
            sufUrl = sufUrl.substring(1, sufUrl.length() - 1);
            if (!sufUrl.startsWith("/")) {
                sufUrl = "/" + sufUrl;
            }
            String url = preUrl + sufUrl;
            methodToApi.put(methodName, url);
        }
        return methodToApi;
    }

    /**
     * 获取 java 文件所在的包名
     * @param javaFile java 文件
     */
    public static String getPackageName(File javaFile) throws FileNotFoundException {
        CompilationUnit compilationUnit = StaticJavaParser.parse(javaFile);
        String packageName = "";
        if (compilationUnit.getPackageDeclaration().isPresent()) {
            PackageDeclaration packageDeclaration = compilationUnit.getPackageDeclaration().get();
            packageName = packageDeclaration.getNameAsString();
        }
        return packageName;
    }

    /**
     * 获取 java 类的全限定类名
     * @param javaFile java 文件
     */
    public static String getFullQualifiedName(File javaFile) throws FileNotFoundException {
        String fullQualifiedName = "";
        CompilationUnit compilationUnit = StaticJavaParser.parse(javaFile);
        if (compilationUnit.getPackageDeclaration().isPresent()) {
            PackageDeclaration packageDeclaration = compilationUnit.getPackageDeclaration().get();
            fullQualifiedName += packageDeclaration.getNameAsString();
        }
        if (compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).isPresent()) {
            fullQualifiedName += "." + compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).get().getNameAsString();
        }
        return fullQualifiedName;
    }

    /**
     * SpringBoot 启动类访问器
     */
    private static class StartupClassVisitor extends VoidVisitorAdapter<List<Object>> {

        @Override
        public void visit(ClassOrInterfaceDeclaration n, List<Object> arg) {
            if (n.getAnnotations() != null) {
                for (AnnotationExpr annotation : n.getAnnotations()) {
                    if ("SpringBootApplication".equals(annotation.getNameAsString())) {
                        arg.add(n);
                    }
                }
            }
        }
    }

    /**
     * 类注解访问器
     */
    public static class ClassAnnotationVisitor extends VoidVisitorAdapter<Object> {

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Object arg) {
            UrlContext urlItem = (UrlContext) arg;
            if (n.getAnnotations() != null) {
                for (AnnotationExpr annotation : n.getAnnotations()) {
                    if (annotation.getName().asString().equals("RequestMapping") ||
                            annotation.getName().asString().equals("PostMapping") ||
                            annotation.getName().asString().equals("GetMapping") ||
                            annotation.getName().asString().equals("PutMapping") ||
                            annotation.getName().asString().equals("DeleteMapping") ||
                            annotation.getName().asString().equals("PatchMapping")) {
                        List<MemberValuePair> memberValuePairs = getMemberPairList(annotation);
                        for (MemberValuePair pair : memberValuePairs) {
                            if (pair.getName().asString().equals("value") || pair.getName().asString().equals("path")) {
                                urlItem.setUrl1(pair.getValue().toString());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取一个注解中的所有参数对
     * key 为参数名 value 为参数值
     */
    public static List<MemberValuePair> getMemberPairList(AnnotationExpr annotation) {
        List<MemberValuePair> memberValuePairs = new LinkedList<>();
        if (annotation.getClass().equals(SingleMemberAnnotationExpr.class)) {
            memberValuePairs.add(new MemberValuePair("value", ((SingleMemberAnnotationExpr) annotation).getMemberValue()));
        } else if (annotation.getClass().equals(NormalAnnotationExpr.class)) {
            memberValuePairs.addAll(((NormalAnnotationExpr) annotation).getPairs());
        }
        return memberValuePairs;
    }

    /**
     * 方法注解访问器
     */
    public static class MethodAnnotationVisitor extends VoidVisitorAdapter<Object> {

        @Override
        public void visit(MethodDeclaration n, Object arg) {
            String method = n.getName().asString();
            if (n.getAnnotations() != null) {
                UrlContext urlItem = (UrlContext) arg;
                method = urlItem.getFullQualifiedName() + "." + method;
                for (AnnotationExpr annotation : n.getAnnotations()) {
                    if (annotation.getName().asString().equals("RequestMapping") ||
                            annotation.getName().asString().equals("PostMapping")||
                            annotation.getName().asString().equals("GetMapping") ||
                            annotation.getName().asString().equals("PutMapping") ||
                            annotation.getName().asString().equals("DeleteMapping") ||
                            annotation.getName().asString().equals("PatchMapping")) {
                        StringBuilder httpMethod = new StringBuilder();
                        if (getHttpMethodByAnnotationType(annotation.getName().asString()) != null) {
                            httpMethod.append(getHttpMethodByAnnotationType(annotation.getName().asString()));
                        }
                        List<MemberValuePair> memberValuePairs = getMemberPairList(annotation);
                        if (!memberValuePairs.isEmpty()) {
                            for (MemberValuePair pair : memberValuePairs) {
                                if (pair.getName().asString().equals("value") || pair.getName().asString().equals("path")) {
                                    urlItem.getUrl2().put(method, pair.getValue().toString());
                                }
                                if (pair.getName().asString().equals("method")) {
                                    if (pair.getValue().isArrayInitializerExpr()) {
                                        for (Expression expression : ((ArrayInitializerExpr) pair.getValue()).getValues()) {
                                            if (httpMethod.length() > 0) {
                                                httpMethod.append(" ").append(expression.toString());
                                            } else {
                                                httpMethod.append(expression.toString());
                                            }
                                        }
                                    }
                                }
                            }
                            if (httpMethod.length() == 0) {
                                for (RequestMethod h : RequestMethod.values()) {
                                    if (httpMethod.length() > 0) {
                                        httpMethod.append(" ").append(h);
                                    } else {
                                        httpMethod.append(h);
                                    }
                                }
                            }
                        } else {
                            urlItem.getUrl2().put(method, "");
                        }
                        urlItem.getHttpMethod().put(method, httpMethod.toString());
                    }
                }
            }
        }
    }

    private static String getHttpMethodByAnnotationType(String annotation) {
        if ("PostMapping".equals(annotation)) {
            return "RequestMethod.POST";
        } else if ("GetMapping".equals(annotation)) {
            return "RequestMethod.GET";
        } else if ("PutMapping".equals(annotation)) {
            return "RequestMethod.PUT";
        } else if ("DeleteMapping".equals(annotation)) {
            return "RequestMethod.DELETE";
        } else if ("PatchMapping".equals(annotation)) {
            return "RequestMethod.PATCH";
        } else {
            return null;
        }
    }

    /**
     * 将匹配的硬编码 IP:PORT 的 URL 替换为微服务名称
     * @param javaFile .java 路径
     * @param microserviceName 微服务名称
     * @param urls 一个微服务模块下声明的 URL 列表 每个元素前面添加了 PORT 提高匹配成功率
     */
    public static void restTemplateUrlReplacer(String javaFile, String microserviceName, List<String> urls, ModificationRecorder recorder) throws IOException {
        Path path = Paths.get(javaFile);
        String javaFileContent = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        CompilationUnit compilationUnit = StaticJavaParser.parse(new File(javaFile));
        List<MethodDeclaration> methodDeclarations = compilationUnit.findAll(MethodDeclaration.class);
        // 针对每个方法声明进行修改
        for (MethodDeclaration methodDeclaration : methodDeclarations) {
            Map<String, StringVariableContext> variableNameAndValues = new LinkedHashMap<>();
            // 获取类成员字段
            getStringFieldDeclaration(compilationUnit.findAll(FieldDeclaration.class), variableNameAndValues);
            // 获取方法中的字符串变量
            getStringVariableNameAndValues(methodDeclaration, variableNameAndValues);
            stringVariableValueReplacer(variableNameAndValues, microserviceName, urls);
            RestTemplateParameterContext restTemplateParameterContext = new RestTemplateParameterContext(microserviceName, urls, variableNameAndValues);
            new HttpRequestStringVisitor().visit(methodDeclaration, restTemplateParameterContext);
            // System.out.println(variableNameAndValues);
        }
        System.out.println(compilationUnit);
        String newJavaFileContent = compilationUnit.toString();
        if (!javaFileContent.equals(newJavaFileContent)) {
            System.out.println("-----\n" + newJavaFileContent);
            Files.write(path, newJavaFileContent.getBytes(StandardCharsets.UTF_8));
            recorder.addRecord(microserviceName,
                    new CodeModification(javaFile, ModificationType.CODE_CHANGE, null, microserviceName, "替换 RestTemplate 请求 URL"));
        }
    }

    private static void getStringFieldDeclaration(List<FieldDeclaration> fieldDeclarations, Map<String, StringVariableContext> variableNameAndValue) {
        for (FieldDeclaration fieldDeclaration : fieldDeclarations) {
            for (VariableDeclarator variableDeclarator : fieldDeclaration.getVariables()) {
                String name = variableDeclarator.getName().asString();
                StringVariableContext stringVariableContext = new StringVariableContext();
                StringBuilder value = new StringBuilder();
                if (variableDeclarator.getInitializer().isPresent()) {
                    if (variableDeclarator.getInitializer().get().isStringLiteralExpr()) {
                        value.append(variableDeclarator.getInitializer().get().asStringLiteralExpr().getValue());
                        stringVariableContext.setCurrent(variableDeclarator.getInitializer().get().asStringLiteralExpr());
                        stringVariableContext.setValue(value.toString());
                        variableNameAndValue.put(name, stringVariableContext);
                    }
                }
            }
        }
    }

    private static void stringVariableValueReplacer(Map<String, StringVariableContext> variableNameAndValues, String microserviceName, List<String> urls) {
        for (String name : variableNameAndValues.keySet()) {
            StringVariableContext stringVariableContext = variableNameAndValues.get(name);
            replaceParentNode(stringVariableContext, microserviceName, urls);
        }
    }

    private static void replaceParentNode(StringVariableContext stringVariableContext, String microserviceName) {
        Matcher matcher = REGEX_PATTERN.matcher(stringVariableContext.getValue());
        if (matcher.find()) {
            StringVariableContext prev = stringVariableContext;
            while (prev.getPrev() != null) {
                prev = prev.getPrev();
            }
            Expression expression = prev.getCurrent();
            if (expression.isVariableDeclarationExpr()) {
                VariableDeclarationExpr variableDeclarationExpr = expression.asVariableDeclarationExpr();
                for (VariableDeclarator variableDeclarator : variableDeclarationExpr.getVariables()) {
                    if (variableDeclarator.getInitializer().isPresent()) {
                        if (variableDeclarator.getInitializer().get().isStringLiteralExpr()) {
                            replaceIpPort(variableDeclarator.getInitializer().get().asStringLiteralExpr(), microserviceName);
                        }
                    }
                }
            } else if (expression.isAssignExpr()) {
                if (expression.asAssignExpr().getValue().isStringLiteralExpr()) {
                    replaceIpPort(expression.asAssignExpr().getValue().asStringLiteralExpr(), microserviceName);
                }
            } else if (expression.isStringLiteralExpr()) {
                replaceIpPort(expression.asStringLiteralExpr(), microserviceName);
            }
        }
    }

    private static void replaceParentNode(StringVariableContext stringVariableContext, String microserviceName, List<String> urls) {
        Matcher matcher = REGEX_PATTERN.matcher(stringVariableContext.getValue());
        if (matcher.find()) {
            StringVariableContext prev = stringVariableContext;
            while (prev.getPrev() != null) {
                prev = prev.getPrev();
            }
            Expression expression = prev.getCurrent();
            if (expression.isVariableDeclarationExpr()) {
                VariableDeclarationExpr variableDeclarationExpr = expression.asVariableDeclarationExpr();
                for (VariableDeclarator variableDeclarator : variableDeclarationExpr.getVariables()) {
                    if (variableDeclarator.getInitializer().isPresent()) {
                        if (variableDeclarator.getInitializer().get().isStringLiteralExpr()) {
                            replaceIpPort(variableDeclarator.getInitializer().get().asStringLiteralExpr(), microserviceName);
                        }
                    }
                }
            } else if (expression.isAssignExpr()) {
                if (expression.asAssignExpr().getValue().isStringLiteralExpr()) {
                    replaceIpPort(expression.asAssignExpr().getValue().asStringLiteralExpr(), microserviceName);
                }
            } else if (expression.isStringLiteralExpr()) {
                replaceIpPort(expression.asStringLiteralExpr(), microserviceName);
            }
        }
    }

    /**
     * 获取一个方法内声明的局部变量值 字符串类型
     * @param methodDeclaration 方法声明
     */
    private static void getStringVariableNameAndValues(MethodDeclaration methodDeclaration, Map<String, StringVariableContext> variableNameAndValue) {
        BlockStmt body = methodDeclaration.getBody().orElse(null);
        if (body != null) {
            // 仅筛选字符串或者字符串拼接的变量
            for (Statement statement : body.getStatements()) {
                if (statement.isExpressionStmt()) {
                    Expression expression = statement.asExpressionStmt().getExpression();
                    if (expression.isVariableDeclarationExpr()) {
                        // 变量声明表达式
                        VariableDeclarationExpr variableDeclarationExpr = expression.asVariableDeclarationExpr();
                        for (VariableDeclarator variableDeclarator : variableDeclarationExpr.getVariables()) {
                            if ("String".equals(variableDeclarator.getType().asString())) {
                                // 已初始化
                                if (variableDeclarator.getInitializer().isPresent()) {
                                    // 是字符串变量
                                    StringVariableContext stringVariableContext = new StringVariableContext(variableDeclarationExpr);
                                    String name = variableDeclarator.getNameAsString();
                                    StringBuilder value = new StringBuilder();
                                    if (variableDeclarator.getInitializer().get().isStringLiteralExpr()) {
                                        value.append(variableDeclarator.getInitializer().get().asStringLiteralExpr().getValue());
                                    } else if (variableDeclarator.getInitializer().get().isBinaryExpr()) { // 字符串拼接
                                        Expression left = getStringBinaryExpr(variableNameAndValue, value,
                                                variableDeclarator.getInitializer().get().asBinaryExpr());
                                        if (left.isStringLiteralExpr()) {
                                            value.insert(0, left.asStringLiteralExpr().getValue());
                                        } else if (left.isNameExpr()) {
                                            setPrev(variableDeclarator.getInitializer().get(), variableNameAndValue, stringVariableContext);
                                            value.insert(0, variableNameAndValue.get(left.asNameExpr().getNameAsString()).getValue());
                                        }
                                    } else if (variableDeclarator.getInitializer().get().isNameExpr()) { // 初始化的值为另一个变量的值
                                        if (variableNameAndValue.containsKey(variableDeclarator.getInitializer().get().asNameExpr().getNameAsString())) {
                                            setPrev(variableDeclarator.getInitializer().get(), variableNameAndValue, stringVariableContext);
                                            value.append(variableNameAndValue.get(variableDeclarator.getInitializer().get().asNameExpr().getNameAsString()).getValue());
                                        }
                                    }
                                    stringVariableContext.setValue(value.toString());
                                    variableNameAndValue.put(name, stringVariableContext);
                                } else { // 未初始化
                                    variableNameAndValue.put(variableDeclarator.getNameAsString(), new StringVariableContext(variableDeclarationExpr, ""));
                                }
                            }
                        }
                    } else if (expression.isAssignExpr()) {
                        AssignExpr assignExpr = expression.asAssignExpr();
                        StringVariableContext stringVariableContext = new StringVariableContext(assignExpr);
                        String name = assignExpr.getTarget().toString();
                        StringBuilder value = new StringBuilder();
                        if (assignExpr.getValue().isStringLiteralExpr()) {
                            value.append(assignExpr.getValue().asStringLiteralExpr().getValue());
                        } else if (assignExpr.getValue().isBinaryExpr()) {
                            Expression left = getStringBinaryExpr(variableNameAndValue, value, assignExpr.getValue().asBinaryExpr());
                            if (left.isStringLiteralExpr()) {
                                value.insert(0, left.asStringLiteralExpr().getValue());
                            } else if (left.isNameExpr()) {
                                if (variableNameAndValue.containsKey(left.asNameExpr().getNameAsString())) {
                                    setPrev(left, variableNameAndValue, stringVariableContext);
                                    value.insert(0, variableNameAndValue.get(left.asNameExpr().getNameAsString()).getValue());
                                }
                            }
                        } else if (assignExpr.getValue().isNameExpr()) {
                            if (variableNameAndValue.containsKey(assignExpr.getValue().asNameExpr().getNameAsString())) {
                                setPrev(assignExpr.getValue(), variableNameAndValue, stringVariableContext);
                                value.append(variableNameAndValue.get(assignExpr.getValue().asNameExpr().getNameAsString()).getValue());
                            }
                        }
                        stringVariableContext.setValue(value.toString());
                        variableNameAndValue.put(name, stringVariableContext);
                    }
                }
            }
        }
    }

    /**
     * 获取字符串常量拼接的表达式的值
     * @param variableNameAndValue 存储前面已经声明的变量名与变量值的映射
     * @param value 拼接后的值
     * @param binaryExpr 字符串拼接表达式
     */
    private static Expression getStringBinaryExpr(Map<String, StringVariableContext> variableNameAndValue, StringBuilder value, Expression binaryExpr) {
        while (binaryExpr.isBinaryExpr()) {
            if (((BinaryExpr) binaryExpr).getRight().isStringLiteralExpr()) {
                value.insert(0, ((BinaryExpr) binaryExpr).getRight().asStringLiteralExpr().getValue());
            } else if (((BinaryExpr) binaryExpr).getRight().isNameExpr()) {
                if (variableNameAndValue.containsKey(((BinaryExpr) binaryExpr).getRight().asNameExpr().getNameAsString())) {
                    value.insert(0, variableNameAndValue.get(((BinaryExpr) binaryExpr).getRight().asNameExpr().getNameAsString()));
                }
            } else {
                value.insert(0, ((BinaryExpr) binaryExpr).getRight().toString());
            }
            binaryExpr = ((BinaryExpr) binaryExpr).getLeft();
        }
        return binaryExpr;
    }

    /**
     * 需要将方法调用中的参数与 URLs 进行匹配 如果匹配则需要将 IP:PORT 替换为微服务名称
     * RestTemplate 方法调用参数访问器
     * 1. 字符串常量 "http://ip:port/path"
     * 2. 字符串常量拼接 "http://ip:port" + "path"
     * 3. 变量拼接 ip:port + path
     * 4. 变量拼接字符串 ip:port + "/path"
     */
    private static class HttpRequestStringVisitor extends VoidVisitorAdapter<RestTemplateParameterContext> {

        /**
         * 访问字符串常量
         */
        @Override
        public void visit(StringLiteralExpr n, RestTemplateParameterContext arg) {
            String microserviceName = arg.getMicroserviceName();
            List<String> urls = arg.getUrls();
            for (String url : urls) {
                // 字符串中包含 URL 则替换
                if (n.getValue().contains(url)) {
                    System.out.println("替换前：" + n.getValue());
                    replaceIpPort(n, microserviceName);
                }
            }
        }

        @Override
        public void visit(BinaryExpr n, RestTemplateParameterContext arg) {
            String microserviceName = arg.getMicroserviceName();
            List<String> urls = arg.getUrls();
            Map<String, StringVariableContext> variableNameAndValues = arg.getVariableNameAndValues();
            Expression binaryExpr = n;
            StringBuilder value = new StringBuilder();
            while (binaryExpr.isBinaryExpr()) {
                Expression right = ((BinaryExpr) binaryExpr).getRight();
                if (right.isStringLiteralExpr()) {
                    value.insert(0, right.asStringLiteralExpr().getValue());
                } else if (right.isNameExpr()) {
                    if (variableNameAndValues.containsKey(right.asNameExpr().getNameAsString())) {
                        if (variableNameAndValues.get(right.asNameExpr().getNameAsString()).getValue() != null) {
                            value.insert(0, variableNameAndValues.get(right.asNameExpr().getNameAsString()).getValue());
                        }
                    }
                }
                binaryExpr = ((BinaryExpr) binaryExpr).getLeft();
            }
            if (binaryExpr.isStringLiteralExpr()) {
                value.insert(0, binaryExpr.asStringLiteralExpr().getValue());
            } else if (binaryExpr.isNameExpr()) {
                if (variableNameAndValues.containsKey(binaryExpr.asNameExpr().getNameAsString())) {
                    if (variableNameAndValues.get(binaryExpr.asNameExpr().getNameAsString()).getValue() != null) {
                        value.insert(0, variableNameAndValues.get(binaryExpr.asNameExpr().getNameAsString()).getValue());
                    }
                }
            }
            for (String url : urls) {
                // 字符串中包含 URL 则替换
                if (value.toString().contains(url)) {
                    binaryExpr = n;
                    while (binaryExpr.isBinaryExpr()) {
                        Expression right = ((BinaryExpr) binaryExpr).getRight();
                        if (right.isStringLiteralExpr()) {
                            replaceIpPort(right.asStringLiteralExpr(), microserviceName);
                        } else if (right.isNameExpr()) {
                            if (variableNameAndValues.containsKey(right.asNameExpr().getNameAsString())) {
                                StringVariableContext stringVariableContext = variableNameAndValues.get(right.asNameExpr().getNameAsString());
                                replaceParentNode(stringVariableContext, microserviceName);
                            }
                        }
                        binaryExpr = ((BinaryExpr) binaryExpr).getLeft();
                    }
                    if (binaryExpr.isStringLiteralExpr()) {
                        replaceIpPort(binaryExpr.asStringLiteralExpr(), microserviceName);
                    } else if (binaryExpr.isNameExpr()) {
                        if (variableNameAndValues.containsKey(binaryExpr.asNameExpr().getNameAsString())) {
                            StringVariableContext stringVariableContext = variableNameAndValues.get(binaryExpr.asNameExpr().getNameAsString());
                            replaceParentNode(stringVariableContext, microserviceName);
                        }
                    }
                }
            }
        }
    }

    private static void replaceIpPort(StringLiteralExpr n, String microserviceName) {
        Matcher matcher = REGEX_PATTERN.matcher(n.getValue());
        if (matcher.find()) {
            System.out.println("before: " + n);
            n.setValue(matcher.replaceAll(microserviceName));
            System.out.println("after: " + n);
        }
    }

    private static void setPrev(Expression expression, Map<String, StringVariableContext> variableNameAndValue, StringVariableContext stringVariableContext) {
        String name = expression.asNameExpr().getNameAsString();
        String value = variableNameAndValue.get(name).getValue();
        Matcher matcher = REGEX_PATTERN.matcher(value);
        if (value != null) {
            if (matcher.find()) {
                stringVariableContext.setPrev(variableNameAndValue.get(name));
            }
        }
    }
}
