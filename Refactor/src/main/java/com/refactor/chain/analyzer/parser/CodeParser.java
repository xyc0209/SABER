package com.refactor.chain.analyzer.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CodeParser {
    private final JavaParser parser;

    public CodeParser(JavaParser parser) {
        this.parser = parser;
    }

    public List<CompilationUnit> parseProject(String projectSrcPath/*, String localMavenRepo*/) {
        List<CompilationUnit> compilationUnits = new ArrayList<>();
        File projectDir = new File(projectSrcPath);
//        File localMavenRepoDir = new File(localMavenRepo);
        List<File> javaFiles = listJavaFiles(projectDir);

        for (File file : javaFiles) {
            try {
                ParseResult<CompilationUnit> result = parser.parse(file);
                if (result.isSuccessful() && result.getResult().isPresent()) {
                    compilationUnits.add(result.getResult().get());
                    System.out.println("解析成功: " + file.getPath());
                } else {
                    System.err.println("解析失败: " + file.getPath());
                }
            } catch (FileNotFoundException e) {
                System.out.println("文件不存在: " + file.getPath());
            }
        }

        return compilationUnits;
    }

    private List<File> listJavaFiles(File dir) {
        List<File> javaFiles = new ArrayList<>();
        if (dir.isDirectory()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                javaFiles.addAll(listJavaFiles(file));
            }
        } else if (dir.isFile() && dir.getName().endsWith(".java")) {
            javaFiles.add(dir);
        }
        return javaFiles;
    }
}
