package com.refactor.chain.utils;

/**
 * @description:
 * @author: xyc
 * @date: 2024-10-21 18:42
 */
public class PathUtils {

    public static String getParentPath(String path) {
        if (path == null || path.isEmpty()) {
            return null; // 或者返回空字符串，根据需求调整
        }

        String separator = "\\"; // Windows 路径分隔符
        String otherSeparator = "/"; // Unix 系统路径分隔符

        // 判断哪个分隔符存在于路径中
        if (path.contains(separator)) {
            // Windows 路径处理
            int lastIndex = path.lastIndexOf(separator);
            return lastIndex != -1 ? path.substring(0, lastIndex) : null;
        } else if (path.contains(otherSeparator)) {
            // Unix 系统路径处理
            int lastIndex = path.lastIndexOf(otherSeparator);
            return lastIndex != -1 ? path.substring(0, lastIndex) : null;
        } else {
            return null; // 如果没有找到分隔符
        }
    }

    public static void main(String[] args) {
        // 测试示例
        String windowsPath = "D:\\software\\apache-maven-3.6.3\\bin\\mvn.cmd";
        String unixPath = "/usr/local/bin/mvn";

        System.out.println("Parent path (Windows): " + getParentPath(windowsPath));
        System.out.println("Parent path (Unix): " + getParentPath(unixPath));
    }
}