package com.refactor.utils;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description: database operation
 * @author: xyc
 * @date: 2024-09-24 10:53
 */
@Component
public class DatabaseUtils {

    public static void splitDatabase(String sourceUrl, String sourceUsername, String sourcePassword, String sourceDatabaseName, String newDatabaseName, List<String> entityList) {
        // 无数据库连接信息
        if (sourceDatabaseName == null)
            return;
        // 源数据库连接信息
        try {
            // 建立源数据库连接
            System.out.println("sourceUrl"+sourceUrl + "sourceUsername" + sourceUsername + "sourcePassword" + sourcePassword);
            Connection sourceConn = DriverManager.getConnection(sourceUrl, sourceUsername, sourcePassword);

            Connection mainConn = DriverManager.getConnection(sourceUrl.substring(0, sourceUrl.lastIndexOf("/")) + "/", sourceUsername, sourcePassword);
            createDatabase(mainConn, newDatabaseName);

            // 建立目标数据库连接
            Connection targetConn = DriverManager.getConnection(sourceUrl.substring(0, sourceUrl.lastIndexOf("/")) + "/" + newDatabaseName, sourceUsername, sourcePassword);

            // 获取所有表名
            ResultSet tableResultSet = sourceConn.getMetaData().getTables(sourceDatabaseName, null, null, new String[]{"TABLE"});
            List<String> tablesWithoutFK = new ArrayList<>();
            List<String> tablesWithFK = new ArrayList<>();

            while (tableResultSet.next()) {
                String tableName = tableResultSet.getString("TABLE_NAME");
                System.out.println("TABLE: " + tableName);

                // 检查是否有外键约束
                if (hasForeignKeyConstraint(sourceConn, sourceDatabaseName, tableName)) {
                    tablesWithFK.add(tableName);
                } else {
                    tablesWithoutFK.add(tableName);
                }
            }

// 在主逻辑中调用方法
            copyTables(sourceConn, targetConn, sourceDatabaseName, newDatabaseName, tablesWithoutFK, entityList);
            copyTables(sourceConn, targetConn, sourceDatabaseName, newDatabaseName, tablesWithFK, entityList);
            System.out.println("数据库复制完成!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void copyTables(Connection sourceConn, Connection targetConn, String sourceDatabaseName, String newDatabaseName, List<String> tables, List<String> entityList) throws SQLException {
        for (String tbl : tables) {
            if (entityList == null || (entityList != null && !entityList.isEmpty() && entityList.stream().map(String::toLowerCase).collect(Collectors.toList()).contains(tbl.toLowerCase()))) {
                // 复制表结构
                copyTableStructure(sourceConn, sourceDatabaseName, targetConn, tbl);
                // 复制表数据
                copyTableData(sourceConn, targetConn, sourceDatabaseName, newDatabaseName, tbl);
            }
        }
    }
    private static boolean hasForeignKeyConstraint(Connection conn, String databaseName, String tableName) throws SQLException {
        String query = "SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND REFERENCED_TABLE_NAME IS NOT NULL";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, databaseName);
            stmt.setString(2, tableName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;  // 如果存在外键约束，返回 true
            }
        }
        return false;  // 否则返回 false
    }
    public  Map<String,String> getDatabaseDetails(FileFactory fileFactory, String path) throws IOException {
        List<String> applicationYamlOrProperties= fileFactory.getApplicationYamlOrPropertities(path);
        String application = null;
//        if(applicationYamlOrProperties.size() == 1)
//            application = applicationYamlOrProperties.get(0);
        return this.getDatabase(applicationYamlOrProperties);
    }


    public static void copyDatabase(Map<String,String> nanoDatabaseDetails, Map<String,String> targetDatabaseDetails, List<String> entityList) {
        // 源数据库连接信息

        try {
            String sourceUrl= nanoDatabaseDetails.get("sourceDatabaseUrl");
            String sourceUsername= nanoDatabaseDetails.get("username");
            String sourcePassword= nanoDatabaseDetails.get("password");
            String sourceDatabaseName= nanoDatabaseDetails.get("sourceDatabaseName");
            // 建立源数据库连接
            System.out.println("sourceUrl"+nanoDatabaseDetails.get("sourceDatabaseUrl") + "sourceUsername" + nanoDatabaseDetails.get("username") + "sourcePassword" + nanoDatabaseDetails.get("password"));
            Connection sourceConn = DriverManager.getConnection(sourceUrl, sourceUsername, sourcePassword);

            Connection mainConn = DriverManager.getConnection(sourceUrl.substring(0, sourceUrl.lastIndexOf("/")) + "/", sourceUsername, sourcePassword);

            String targetUrl= targetDatabaseDetails.get("sourceDatabaseUrl");
            String targetUsername= targetDatabaseDetails.get("username");
            String targetPassword= targetDatabaseDetails.get("password");
            String targetDatabaseName= targetDatabaseDetails.get("sourceDatabaseName");
            // 建立目标数据库连接
            Connection targetConn = DriverManager.getConnection(sourceUrl.substring(0, sourceUrl.lastIndexOf("/")) + "/" + targetDatabaseName, targetUsername, targetPassword);

            // 获取所有表名
            ResultSet tableResultSet = sourceConn.getMetaData().getTables(sourceDatabaseName, null, null, new String[]{"TABLE"});
            while (tableResultSet.next()) {
                String tableName = tableResultSet.getString("TABLE_NAME");
                System.out.println("TABLE" +tableName);
                if(entityList !=null && !entityList.isEmpty()){
                    if (entityList.stream()
                            .map(String::toLowerCase)
                            .collect(Collectors.toList()).contains(tableName.toLowerCase())){
                        // 复制表结构
                        copyTableStructure(sourceConn, sourceDatabaseName, targetConn, tableName);

                        // 复制表数据
                        copyTableData(sourceConn, targetConn, sourceDatabaseName, targetDatabaseName, tableName);
                    }
                }
                else {
                    // 复制表结构
                    copyTableStructure(sourceConn, sourceDatabaseName, targetConn, tableName);

                    // 复制表数据
                    copyTableData(sourceConn, targetConn, sourceDatabaseName, targetDatabaseName, tableName);
                }
            }

            System.out.println("数据库复制完成!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createDatabase(Connection conn, String databaseName) throws SQLException {
        String createDatabaseQuery = "CREATE DATABASE IF NOT EXISTS " + databaseName;
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createDatabaseQuery);
        }
    }
    private static void copyTableStructure(Connection sourceConn, String sourceDatabaseName, Connection targetConn, String tableName) throws SQLException {
        // 禁用外键检查
        try (Statement disableFKCheckStmt = targetConn.createStatement()) {
            disableFKCheckStmt.execute("SET foreign_key_checks = 0");
        }

        // 使用 SHOW CREATE TABLE 语句获取建表 SQL
        String showCreateTableQuery = "SHOW CREATE TABLE " + sourceDatabaseName + "." + tableName;
        System.out.println("showCreateTableQuery: " + showCreateTableQuery);

        try (Statement stmt = sourceConn.createStatement();
             ResultSet resultSet = stmt.executeQuery(showCreateTableQuery)) {
            if (resultSet.next()) {
                String createTableSql = resultSet.getString(2);
                System.out.println("createTableSql: " + createTableSql);
                // 在目标数据库中执行建表 SQL
                try (PreparedStatement createTableStmt = targetConn.prepareStatement(createTableSql)) {
                    createTableStmt.executeUpdate();
                }
            }
        } finally {
            // 启用外键检查
            try (Statement enableFKCheckStmt = targetConn.createStatement()) {
                enableFKCheckStmt.execute("SET foreign_key_checks = 1");
            }
        }
    }

    private static void copyTableData(Connection sourceConn, Connection targetConn, String sourceDatabaseName, String newDatabaseName, String tableName) throws SQLException {
        // 禁用外键检查
        try (Statement disableFKCheckStmt = targetConn.createStatement()) {
            disableFKCheckStmt.execute("SET foreign_key_checks = 0");
        }

        String selectQuery = "SELECT * FROM " + sourceDatabaseName + "." + tableName;
        int columnCount = getColumnCount(sourceConn, sourceDatabaseName, tableName);
        String insertQuery = buildInsertQuery(sourceConn, newDatabaseName, tableName, columnCount);

        System.out.println("columnCount: " + columnCount);

        try (PreparedStatement selectStmt = sourceConn.prepareStatement(selectQuery);
             PreparedStatement insertStmt = targetConn.prepareStatement(insertQuery)) {
            ResultSet resultSet = selectStmt.executeQuery();
            while (resultSet.next()) {
                // 设置插入语句的参数值
                for (int i = 1; i <= columnCount; i++) {
                    insertStmt.setObject(i, resultSet.getObject(i));
                }
                insertStmt.executeUpdate();
            }
        } finally {
            // 启用外键检查
            try (Statement enableFKCheckStmt = targetConn.createStatement()) {
                enableFKCheckStmt.execute("SET foreign_key_checks = 1");
            }
        }
    }

    private static int getColumnCount(Connection conn, String databaseName, String tableName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        String sql = "SELECT COUNT(*) AS column_count " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE table_name = ? AND table_schema = ?";
        try (PreparedStatement selectStmt = conn.prepareStatement(sql)) {
            selectStmt.setString(1, tableName);
            selectStmt.setString(2, databaseName);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("column_count");
                } else {
                    return 0;
                }
            }
        }
//        try (ResultSet columnRS = metaData.getColumns(null, databaseName, tableName, null)) {
//            while (columnRS.next()) {
//                String columnName = columnRS.getString("COLUMN_NAME");
//                System.out.println("columnName"+columnName);
//            }
//            ResultSetMetaData columnRSMetaData = columnRS.getMetaData();
//            System.out.println("columnRSMetaData.getColumnCount()"+columnRSMetaData.getColumnCount());
//            return columnRSMetaData.getColumnCount();
//        }
//        String selectQuery = "SELECT * FROM testdb." + tableName + "LIMIT 1";
//        PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
//
////        System.out.println("selectStmt.getMetaData().getColumnCount()"+selectStmt.getMetaData().getColumnCount());
//        System.out.println("selectStmt.getMetaData()"+selectStmt.getMetaData());
//        return selectStmt.getMetaData().getColumnCount();
//        String sql = "SELECT COUNT(*) AS column_count FROM INFORMATION_SCHEMA.COLUMNS WHERE table_name = ?";
//        try (PreparedStatement selectStmt = conn.prepareStatement(sql)) {
//            selectStmt.setString(1, tableName);
//            try (ResultSet rs = selectStmt.executeQuery()) {
//                if (rs.next()) {
//                    return rs.getInt("column_count");
//                } else {
//                    return 0;
//                }
//            }
//        }
    }

    private static String buildInsertQuery(Connection conn, String newDatabaseName, String tableName, int columnCount) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
//        try (ResultSet columnRS = metaData.getColumns(null, null, tableName, null)) {
//            int columnCount = 0;
//            while (columnRS.next()) {
//                columnCount++;
//            }

            StringBuilder sb = new StringBuilder("INSERT INTO " + newDatabaseName + "." + tableName + " VALUES (");
            for (int i = 0; i < columnCount; i++) {
                sb.append("?");
                if (i < columnCount - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
            System.out.println("columnCount"+columnCount);
            return sb.toString();
        }


    public Map<String, String> getDatabase(List<String> applicationYamlOrProperties) throws IOException {
        HashMap<String, String> databaseDetails = new HashMap<>();
        String serviceName = null;
        String username = null;
        String password = null;
        String sourceDatabaseUrl = null;
        String sourceDatabaseName = null;
        Yaml yaml = new Yaml();
        for (String application: applicationYamlOrProperties) {
            HashMap<String, ArrayList<String>> databaseMap = new HashMap<>();
            if (application.endsWith("application.yaml") || application.endsWith("application.yml") || application.endsWith("application-dev.yml")) {
                Map map = yaml.load(new FileInputStream(application));
                Map mSpring = (Map) map.get("spring");
                Map mApplication = (Map) mSpring.get("application");
                if (mApplication != null)
                    serviceName = (String) mApplication.get("name");
                Map mDatasource = (Map) mSpring.get("datasource");
                if (mDatasource != null) {
                    username = (String) mDatasource.get("username");
                    password = (String) mDatasource.get("password");
                }

            } else {
                InputStream in = new BufferedInputStream(new FileInputStream(application));
                Properties p = new Properties();
                p.load(in);
                serviceName = (String) p.get("spring.application.name");
                username = (String) p.get("spring.datasource.username");
                password = (String) p.get("spring.datasource.password");

            }
            if (username == null)
                continue;
            if (username.contains("$"))
                username = username.replaceAll("\\$\\{[^:]+:|\\}", "");
            if (password.contains("$"))
                password = password.replaceAll("\\$\\{[^:]+:|\\}", "");
            BufferedReader reader = new BufferedReader(new FileReader(application));
            String line = reader.readLine();
            String pattern = "mysql://";
            String target = "";
            int row = 0;
            while (line != null) {
                if (line.contains(pattern)) {
                    int startIndex = line.indexOf(pattern) + 8;
                    int endIndex = line.indexOf("?");
                    if (line.contains("///")) {
                        startIndex = line.indexOf("///") + 3;
                        target = "localhost:3306/" + line.substring(startIndex, endIndex);
                    } else if (line.contains("127.0.0.1")) {
                        startIndex = line.indexOf("//") + 2;
                        target = line.substring(startIndex, endIndex);
                        target = target.replace("localhost", "127.0.0.1");
                    } else {
                        target = line.substring(startIndex, endIndex);
                    }
                    if (databaseMap.containsKey(target)) {
                        databaseMap.get(target).add(serviceName);
                    } else {
                        databaseMap.put(target, new ArrayList<>());
                        databaseMap.get(target).add(serviceName);
                    }
                }
                line = reader.readLine();
            }
            if (target.contains("$")) {
                sourceDatabaseUrl = target.replaceAll("\\$\\{[^:]+:|\\}", "");
                System.out.println(sourceDatabaseUrl);  // 输出: 172.16.17.38:3306:testServiceDB
                int lastSlashIndex = sourceDatabaseUrl.lastIndexOf('/');
                if (lastSlashIndex != -1 && lastSlashIndex < sourceDatabaseUrl.length() - 1) {
                    sourceDatabaseName = sourceDatabaseUrl.substring(lastSlashIndex + 1);
                    System.out.println("sourceDatabaseName" + sourceDatabaseName);
                }
            }
            if (sourceDatabaseUrl != null) {
                databaseDetails.put("serviceName", serviceName);
                databaseDetails.put("username", username);
                databaseDetails.put("password", password);
                databaseDetails.put("sourceDatabaseUrl", "jdbc:mysql://" + sourceDatabaseUrl);
                databaseDetails.put("sourceDatabaseName", sourceDatabaseName);
                System.out.println(databaseDetails.toString());
                return databaseDetails;
            }
        }
        return databaseDetails;
    }



    public static void main(String[] args) throws IOException, SQLException {
//        Connection sourceConn = DriverManager.getConnection("jdbc:mysql://172.16.17.38:3306/testServiceDB", "root", "passwordA123$");

//        DatabaseUtils databaseUtils = new DatabaseUtils();
//        FileFactory fileFactory = new FileFactory();
//        List<String> applicationYamlOrProperties= fileFactory.getApplicationYamlOrPropertities("D:\\code\\Service-Demo\\testService");
//        System.out.println();
//        Yaml yaml = new Yaml();
//        HashMap<String, ArrayList<String>> databaseMap = new HashMap<>();
//        HashMap<String,ArrayList<String>> ServiceIntimacyMap = new HashMap<>();
//        for(String app: applicationYamlOrProperties){
//            databaseUtils.getDatabase(app);
//            System.out.println();
//            String serviceName = "";
//            String username = "";
//            String password = "";
//            System.out.println("app"+app);
//            if(app.endsWith("yaml") || app.endsWith("yml")){
//                Map map = yaml.load(new FileInputStream(app));
//                Map m1 =(Map)map.get("spring");
//                Map m2 = (Map)m1.get("application");
//                if(m2 != null)
//                    serviceName = (String)m2.get("name");
//                Map m3 = (Map)m1.get("datasource");
//                if(m3 != null) {
//                    username = (String) m3.get("username");
//                    password = (String) m3.get("password");
//                }
//                if(username.contains("$"))
//                    username = username.replaceAll("\\$\\{[^:]+:|\\}", "");
//                if(password.contains("$"))
//                    password = password.replaceAll("\\$\\{[^:]+:|\\}", "");
//                System.out.println("USERNAME"+username);
//                System.out.println("password"+password);
//            }
//            else{
//                InputStream in = new BufferedInputStream(new FileInputStream(app));
//                Properties p = new Properties();
//                p.load(in);
//                serviceName = (String)p.get("spring.application.name");
//            }
//            System.out.println("name"+serviceName);
//            BufferedReader reader = new BufferedReader(new FileReader(app));
//            String line = reader.readLine();
//            String pattern = "mysql://";
//            String target = "";
//            int row = 0;
//            while (line != null) {
//                if (line.contains(pattern)) {
//                    int startIndex = line.indexOf(pattern) + 8;
//                    int endIndex = line.indexOf("?");
//                    if (line.contains("///")) {
//                        startIndex = line.indexOf("///") + 3;
//                        target = "localhost:3306/" + line.substring(startIndex, endIndex);
//                    }
//                    else if(line.contains("127.0.0.1")){
//                        startIndex = line.indexOf("//")+2;
//                        target = line.substring(startIndex, endIndex);
//                        target = target.replace("localhost","127.0.0.1");
//                    }
//                    else {
//                        target = line.substring(startIndex, endIndex);
//                    }
//                    System.out.println("target"+target);
////                    if (databaseMap.containsKey(target)) {
////                        databaseMap.get(target).add(serviceName);
////                    } else {
////                        databaseMap.put(target, new ArrayList<>());
////                        databaseMap.get(target).add(serviceName);
////                    }
////                    if (ServiceIntimacyMap.containsKey(serviceName)) {
////                        ServiceIntimacyMap.get(serviceName).add(target);
////                    } else {
////                        ServiceIntimacyMap.put(serviceName, new ArrayList<>());
////                        ServiceIntimacyMap.get(serviceName).add(target);
////                    }
//                }
//                line = reader.readLine();
//            }
//            if(target.contains("$")){
//                String sourceDatabaseUrl = target.replaceAll("\\$\\{[^:]+:|\\}", "");
//                System.out.println(sourceDatabaseUrl);  // 输出: 172.16.17.38:3306:testServiceDB
//                int lastSlashIndex = sourceDatabaseUrl.lastIndexOf('/');
//                if (lastSlashIndex != -1 && lastSlashIndex < sourceDatabaseUrl.length() - 1) {
//                    String sourceDatabaseName = sourceDatabaseUrl.substring(lastSlashIndex + 1);
//                    System.out.println("sourceDatabaseName"+sourceDatabaseName);
//                }
//            }
//        }


    }
}