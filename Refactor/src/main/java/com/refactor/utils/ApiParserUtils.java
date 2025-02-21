package com.refactor.utils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.refactor.dto.ApiVersionContext;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @description:
 * @author: xyc
 * @date: 2024-12-09 19:27
 */
public class ApiParserUtils {
    public void inspectJavaFile(File pFile, ApiVersionContext apiVersionContext, String serviceName,  Map<String, Map<String,UrlItem>> navDetails)
            throws FileNotFoundException, ParseException, IOException {
        ParseResult<CompilationUnit> cu;
        FileInputStream in = new FileInputStream(pFile);
        try {
            cu = new JavaParser().parse(in);
        } finally {
            in.close();
        }
        UrlItem urlItem = new UrlItem();
        new ClassVisitor().visit(cu.getResult().get(), urlItem);
        new MethodVisitor().visit(cu.getResult().get(), urlItem);
        String preUrl = "";
        if(urlItem.getUrl1() != null) {
            preUrl = urlItem.getUrl1().substring(1, urlItem.getUrl1().length() - 1);
            System.out.println("urlItem"+urlItem.toString());
            System.out.println("urlItem.getUrl2().keySet()"+urlItem.getUrl2().keySet());
        }
        for(String methodName: urlItem.getUrl2().keySet()){
            String afterUrl= urlItem.getUrl2().get(methodName);
            System.out.println("methodName---"+methodName);
            System.out.println("afterUrl--"+afterUrl);
            if(afterUrl == null){
                apiVersionContext.getMissingUrlMap().get(serviceName).put(methodName,preUrl);
                continue;
            }
            if(afterUrl.equals("")){
                apiVersionContext.getMissingUrlMap().get(serviceName).put(methodName,preUrl);
                if(!this.apiPattern(preUrl)) {
                    apiVersionContext.getUnversionedMap().get(serviceName).put(methodName, preUrl);
                    if (!navDetails.containsKey(serviceName))
                        navDetails.put(serviceName,new HashMap<>());
                    navDetails.get(serviceName).put(pFile.getAbsolutePath(), urlItem);
                }
                continue;
            }
            afterUrl = afterUrl.substring(1,afterUrl.length()-1);
            String fullUrl = preUrl + afterUrl;
            if(!this.apiPattern(fullUrl)) {
                apiVersionContext.getUnversionedMap().get(serviceName).put(methodName, fullUrl);
                if (!navDetails.containsKey(serviceName))
                    navDetails.put(serviceName,new HashMap<>());
                navDetails.get(serviceName).put(pFile.getAbsolutePath(), urlItem);
            }
        }
    }

    private static class ClassVisitor extends VoidVisitorAdapter {

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Object arg) {
            if(n.getAnnotations() != null){
                for (AnnotationExpr annotation : n.getAnnotations()) {
                    if(annotation.getClass().equals(SingleMemberAnnotationExpr.class)){
                        if(annotation.getName().asString().equals("RequestMapping") ||
                                annotation.getName().asString().equals("PostMapping") ||
                                annotation.getName().asString().equals("GetMapping") ||
                                annotation.getName().asString().equals("PutMapping") ||
                                annotation.getName().asString().equals("DeleteMapping") ||
                                annotation.getName().asString().equals("PatchMapping")
                        ){
                            UrlItem urlItem = (UrlItem) arg;
                            urlItem.setUrl1(((SingleMemberAnnotationExpr) annotation).getMemberValue().toString());
                            return;
                        }
                    }
                    else if (annotation.getClass().equals(NormalAnnotationExpr.class)) {
                        if (annotation.getName().asString().equals("RequestMapping") ||
                                annotation.getName().asString().equals("PostMapping") ||
                                annotation.getName().asString().equals("GetMapping") ||
                                annotation.getName().asString().equals("PutMapping") ||
                                annotation.getName().asString().equals("DeleteMapping") ||
                                annotation.getName().asString().equals("PatchMapping")) {
                            for (MemberValuePair pair : ((NormalAnnotationExpr) annotation).getPairs()) {
                                if (pair.getName().asString().equals("value") || pair.getName().asString().equals("path")) {
                                    UrlItem urlItem = (UrlItem) arg;
                                    urlItem.setUrl1(pair.getValue().toString());
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }



    private static class MethodVisitor extends VoidVisitorAdapter {

        @Override
        public void visit(MethodDeclaration n, Object arg) {
            if (n.getAnnotations() != null) {
                for (AnnotationExpr annotation : n.getAnnotations()) {
                    System.out.println("annotation.getClass()"+annotation.getClass());
                    System.out.println("annotation.getName().asString()"+annotation.getName().asString());
                    System.out.println("n.getName().asString()"+n.getName().asString());
                    if(annotation.getClass().equals(SingleMemberAnnotationExpr.class)){
                        if(annotation.getName().asString().equals("RequestMapping") ||
                                annotation.getName().asString().equals("PostMapping")||
                                annotation.getName().asString().equals("GetMapping") ||
                                annotation.getName().asString().equals("PutMapping") ||
                                annotation.getName().asString().equals("DeleteMapping") ||
                                annotation.getName().asString().equals("PatchMapping")){
                            UrlItem urlItem = (UrlItem) arg;
                            String url2 = ((SingleMemberAnnotationExpr) annotation).getMemberValue().toString();
                            System.out.println("n.getName().asString()"+n.getName().asString());
                            System.out.println("url2"+url2);
                            urlItem.getUrl2().put(n.getName().asString(), url2);
                        }
                    }
                    else if (annotation.getClass().equals(NormalAnnotationExpr.class)) {
                        if (annotation.getName().asString().equals("RequestMapping") ||
                                annotation.getName().asString().equals("PostMapping") ||
                                annotation.getName().asString().equals("GetMapping") ||
                                annotation.getName().asString().equals("PutMapping") ||
                                annotation.getName().asString().equals("DeleteMapping") ||
                                annotation.getName().asString().equals("PatchMapping")) {
                            if(((NormalAnnotationExpr) annotation).getPairs().size() == 0){
                                UrlItem urlItem = (UrlItem) arg;
                                urlItem.getUrl2().put(n.getName().asString(),"");
                                return;
                            }
                            for (MemberValuePair pair : ((NormalAnnotationExpr) annotation).getPairs()) {
                                if (pair.getName().asString().equals("value") || pair.getName().asString().equals("path")) {
                                    UrlItem urlItem = (UrlItem) arg;
                                    urlItem.getUrl2().put(n.getName().asString(), pair.getValue().toString());
                                    return;
                                }
                            }
                        }
                    } else if (annotation.getClass().equals(MarkerAnnotationExpr.class)) {
                        if(annotation.getName().asString().equals("RequestMapping") ||
                                annotation.getName().asString().equals("PostMapping")||
                                annotation.getName().asString().equals("GetMapping") ||
                                annotation.getName().asString().equals("PutMapping") ||
                                annotation.getName().asString().equals("DeleteMapping") ||
                                annotation.getName().asString().equals("PatchMapping")){
                            UrlItem urlItem = (UrlItem) arg;
                            String url2 = "";
                            System.out.println("n.getName().asString()"+n.getName().asString());
                            System.out.println("url2"+url2);
                            urlItem.getUrl2().put(n.getName().asString(), url2);
                            return;
                        }

                    }
                }
            }
        }
    }

    public boolean apiPattern(String apiPath){
        String pattern = "^(?!.*v\\.\\d+).*\\/v([0-9]*[a-z]*\\.*)+([0-9]|[a-z])+\\/.*$";
        Pattern p= Pattern.compile(pattern);
        if(p.matcher(apiPath).matches()){
            return true;
        }
        else{
            return false;
        }

    }


}

    private static class MethodVisitor extends VoidVisitorAdapter {

        @Override
        public void visit(MethodDeclaration n, Object arg) {
            if (n.getAnnotations() != null) {
                for (AnnotationExpr annotation : n.getAnnotations()) {
                    System.out.println("annotation.getClass()"+annotation.getClass());
                    System.out.println("annotation.getName().asString()"+annotation.getName().asString());
                    System.out.println("n.getName().asString()"+n.getName().asString());
                    if(annotation.getClass().equals(SingleMemberAnnotationExpr.class)){
                        if(annotation.getName().asString().equals("RequestMapping") ||
                                annotation.getName().asString().equals("PostMapping")||
                                annotation.getName().asString().equals("GetMapping") ||
                                annotation.getName().asString().equals("PutMapping") ||
                                annotation.getName().asString().equals("DeleteMapping") ||
                                annotation.getName().asString().equals("PatchMapping")){
                            UrlItem urlItem = (UrlItem) arg;
                            String url2 = ((SingleMemberAnnotationExpr) annotation).getMemberValue().toString();
                            System.out.println("n.getName().asString()"+n.getName().asString());
                            System.out.println("url2"+url2);
                            urlItem.getUrl2().put(n.getName().asString(), url2);
                        }
                    }
                    else if (annotation.getClass().equals(NormalAnnotationExpr.class)) {
                        if (annotation.getName().asString().equals("RequestMapping") ||
                                annotation.getName().asString().equals("PostMapping") ||
                                annotation.getName().asString().equals("GetMapping") ||
                                annotation.getName().asString().equals("PutMapping") ||
                                annotation.getName().asString().equals("DeleteMapping") ||
                                annotation.getName().asString().equals("PatchMapping")) {
                            if(((NormalAnnotationExpr) annotation).getPairs().size() == 0){
                                UrlItem urlItem = (UrlItem) arg;
                                urlItem.getUrl2().put(n.getName().asString(),"");
                                return;
                            }
                            for (MemberValuePair pair : ((NormalAnnotationExpr) annotation).getPairs()) {
                                if (pair.getName().asString().equals("value") || pair.getName().asString().equals("path")) {
                                    UrlItem urlItem = (UrlItem) arg;
                                    urlItem.getUrl2().put(n.getName().asString(), pair.getValue().toString());
                                    return;
                                }
                            }
                        }
                    } else if (annotation.getClass().equals(MarkerAnnotationExpr.class)) {
                        if(annotation.getName().asString().equals("RequestMapping") ||
                                annotation.getName().asString().equals("PostMapping")||
                                annotation.getName().asString().equals("GetMapping") ||
                                annotation.getName().asString().equals("PutMapping") ||
                                annotation.getName().asString().equals("DeleteMapping") ||
                                annotation.getName().asString().equals("PatchMapping")){
                            UrlItem urlItem = (UrlItem) arg;
                            String url2 = "";
                            System.out.println("n.getName().asString()"+n.getName().asString());
                            System.out.println("url2"+url2);
                            urlItem.getUrl2().put(n.getName().asString(), url2);
                            return;
                        }

                    }
                }
            }
        }
    }

    public void updateControllerFile(String controllerPath,  UrlItem urlItem, String version) throws FileNotFoundException {
        Charset charset = Charset.forName("UTF-8");
        CompilationUnit cu = StaticJavaParser.parse(new File(controllerPath), charset);
        if (urlItem.getUrl1() != null)
            new UpdateClassVisitor().visit(cu, version);
        else
            new UpdateMethodVisitor().visit(cu, version);
        String code = cu.toString();
        System.out.println("CODE "+ code);
        System.out.println("controllerPath" +controllerPath);
        // 将字符串写回到文件
        try (FileWriter writer = new FileWriter(controllerPath)) {
            writer.write(code);
        } catch (IOException e) {
            e.printStackTrace(); // 处理异常
        }

    }

    private static class UpdateMethodVisitor extends VoidVisitorAdapter {

        @Override
        public void visit(MethodDeclaration n, Object arg) {
            if (n.getAnnotations() != null) {
                for (AnnotationExpr annotation : n.getAnnotations()) {
                    System.out.println("annotation.getClass()"+annotation.getClass());
                    System.out.println("annotation.getName().asString()"+annotation.getName().asString());
                    System.out.println("n.getName().asString()"+n.getName().asString());
                    if(annotation.getClass().equals(SingleMemberAnnotationExpr.class)){
                        if(annotation.getName().asString().equals("RequestMapping") ||
                                annotation.getName().asString().equals("PostMapping")||
                                annotation.getName().asString().equals("GetMapping") ||
                                annotation.getName().asString().equals("PutMapping") ||
                                annotation.getName().asString().equals("DeleteMapping") ||
                                annotation.getName().asString().equals("PatchMapping")){
                            String url2 = ((SingleMemberAnnotationExpr) annotation).getMemberValue().toString();
                            System.out.println("n.getName().asString()"+n.getName().asString());
                            System.out.println("url2"+url2);
                            ((SingleMemberAnnotationExpr) annotation).setMemberValue(new StringLiteralExpr(addVersion(((SingleMemberAnnotationExpr) annotation).getMemberValue().toString(), (String)arg)));
                        }
                    }
                    else if (annotation.getClass().equals(NormalAnnotationExpr.class)) {
                        if (annotation.getName().asString().equals("RequestMapping") ||
                                annotation.getName().asString().equals("PostMapping") ||
                                annotation.getName().asString().equals("GetMapping") ||
                                annotation.getName().asString().equals("PutMapping") ||
                                annotation.getName().asString().equals("DeleteMapping") ||
                                annotation.getName().asString().equals("PatchMapping")) {
                            if(((NormalAnnotationExpr) annotation).getPairs().size() == 0){
                                UrlItem urlItem = (UrlItem) arg;
                                urlItem.getUrl2().put(n.getName().asString(),"");
                                return;
                            }
                            for (MemberValuePair pair : ((NormalAnnotationExpr) annotation).getPairs()) {
                                if (pair.getName().asString().equals("value") || pair.getName().asString().equals("path")) {
                                    ((SingleMemberAnnotationExpr) annotation).setMemberValue(new StringLiteralExpr(addVersion(pair.getValue().toString(), (String)arg)));
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    public static class UpdateClassVisitor extends VoidVisitorAdapter {

        @Override
        public void visit(ClassOrInterfaceDeclaration n, Object arg) {
            if (n.getAnnotations() != null) {
                for (AnnotationExpr annotation : n.getAnnotations()) {
                    if (annotation.getClass().equals(SingleMemberAnnotationExpr.class)) {
                        if (annotation.getName().asString().equals("RequestMapping") ||
                                annotation.getName().asString().equals("PostMapping") ||
                                annotation.getName().asString().equals("GetMapping") ||
                                annotation.getName().asString().equals("PutMapping") ||
                                annotation.getName().asString().equals("DeleteMapping") ||
                                annotation.getName().asString().equals("PatchMapping")
                        ) {
// Set new URL value
                            ((SingleMemberAnnotationExpr) annotation).setMemberValue(new StringLiteralExpr(addVersion(((SingleMemberAnnotationExpr) annotation).getMemberValue().toString(), (String)arg)));
                            return;
                        }
                    } else if (annotation.getClass().equals(NormalAnnotationExpr.class)) {
                        if (annotation.getName().asString().equals("RequestMapping") ||
                                annotation.getName().asString().equals("PostMapping") ||
                                annotation.getName().asString().equals("GetMapping") ||
                                annotation.getName().asString().equals("PutMapping") ||
                                annotation.getName().asString().equals("DeleteMapping") ||
                                annotation.getName().asString().equals("PatchMapping")) {
                            for (MemberValuePair pair : ((NormalAnnotationExpr) annotation).getPairs()) {
                                if (pair.getName().asString().equals("value") || pair.getName().asString().equals("path")) {
                                    ((SingleMemberAnnotationExpr) annotation).setMemberValue(new StringLiteralExpr(addVersion(pair.getValue().toString(), (String)arg)));
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }



    }

    public static String addVersion(String value, String version) {
        int index;
        if(version == null)
            version = "v1";
        if (value.contains("api/")) {
            index = value.indexOf("api/");
            return value.substring(1, index+4)  + version + "/" + value.substring(index + 4, value.length() -1);
        } else {
            return value.substring(1, 2) + version + "/" + value.substring(2, value.length() -1);
        }

    }
    public boolean apiPattern(String apiPath){
        String pattern = "^(?!.*v\\.\\d+).*\\/v([0-9]*[a-z]*\\.*)+([0-9]|[a-z])+\\/.*$";  //   .*/v([0-9]+\.)+[0-9]+/.*   .*/v([0-9]*[a-z]*\.)+([0-9]|[a-z])+/.*
        Pattern p= Pattern.compile(pattern);
        if(p.matcher(apiPath).matches()){
            return true;
        }
        else{
            return false;
        }

    }

}
