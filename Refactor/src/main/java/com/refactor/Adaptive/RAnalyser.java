package com.refactor.Adaptive;

import com.github.javaparser.ParseException;
import com.refactor.config.DependenciesConfig;
import com.refactor.detector.SharedDatabaseAndServiceIntimacyService;
import com.refactor.dto.ApiVersionContext;
import com.refactor.dto.RequestItem;
import com.refactor.context.SharedDatabaseContext;
import com.refactor.trace.RequestChain;
import com.refactor.trace.SvcTransRes;
import com.refactor.trace.TraceChainNode;
import com.refactor.trace.TraceDetail;
import com.refactor.utils.*;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RAnalyser {

    @Value("${detector.ipandPort}")
    private  String detectorIPandPort;

    @Autowired
    private RestTemplate restTemplate;

    public SharedDatabaseContext sharedDatabaseContext;

    @Autowired
    public SharedDatabaseAndServiceIntimacyService sharedDatabaseAndServiceIntimacyService;

    @Resource
    private DependenciesConfig dependenciesConfig;

     public List<String> detectMS(Map<String, String> namePathMap, Map<String, List<String>> svcEntityMap) throws IOException, InterruptedException {
        //If any algorithm detects, add it to filteredSvcs
        // MARS strategy
//         System.out.println("namePathMap "+namePathMap.toString());
         Map<String, Integer> svcLocs= FileFactory.getSvcLocs(namePathMap);
//         System.out.println("svcLocs" +svcLocs.toString());
         int totalLines = svcLocs.values().stream()
                 .mapToInt(Integer::intValue)
                 .sum();  // 求和
         double threshold = totalLines * 0.39;
         System.out.println("threshold"+threshold);
         List<String> filteredSvcs = svcLocs.entrySet().stream()
                 .filter(entry ->{
                     System.out.println("entry.getValue()" +entry.getValue());
                     return entry.getValue() > threshold;} )  // Filter condition
                 .map(Map.Entry::getKey)
                 .collect(Collectors.toList());
         System.out.println("filteredSvcs --mars"+filteredSvcs.toString());
         if (svcEntityMap == null)
             return filteredSvcs;
         //Static Analysis strategy
         int totalSize = svcEntityMap.values().stream()
                 .mapToInt(List::size)  // 获取每个 List 的 size
                 .sum();  // 求和
         // 计算 Map 中 List 的数量（即条目数量）
//         System.out.println("svcEntityMap.toString"+svcEntityMap.toString());
         int totalLists = svcEntityMap.size();
         // 计算平均值
         double averageSize = totalLists > 0 ? (double) totalSize / totalLists : 0;
         double standardDeviation = FileFactory.getAvgEntitys(svcEntityMap, averageSize);
         double threshold2 = 3 * standardDeviation;

         for (Map.Entry<String, List<String>> entry : svcEntityMap.entrySet()) {
             int size = entry.getValue().size();
             String servicePath = namePathMap.get(entry.getKey());
             double difference = Math.abs(size - averageSize);  // 计算差值的绝对值
//             System.out.println("difference: "+difference + "threshold: " + threshold2);
             if (difference > threshold2 && !filteredSvcs.contains(servicePath)) {
                 filteredSvcs.add(servicePath);  // 记录超出阈值的项
             }
         }
         return filteredSvcs;
    }

    public Map<String,List<String>> detectNano(Map<String, String> namePathMap, Map<String, List<String>> svcEntityMap) throws IOException, InterruptedException {

        Map<String,List<String>> nanoPaths =new HashMap<>();
        nanoPaths.put("nano",new ArrayList<>());
        nanoPaths.put("normal",new ArrayList<>());
        // MARS策略
//        System.out.println("namePathMap " + namePathMap.toString());
        Map<String, Integer> svcLocs = FileFactory.getSvcLocs(namePathMap);
        Map<String, Long> svcFiles = FileFactory.getSvcFiles(namePathMap);
//        System.out.println("svcLocs" +svcLocs.toString());
//        System.out.println("svcFiles" +svcFiles.toString());
        int totalLines = svcLocs.values().stream()
                .mapToInt(Integer::intValue)
                .sum();  // 求和
        long totalFiles = svcFiles.values().stream()
                .mapToLong(Long::longValue)
                .sum();  // 求和
        double threshold = (double) totalLines / namePathMap.size() * 0.5;
        double threshold2 = (double) totalFiles / namePathMap.size() * 0.5;
//        System.out.println("threshold "+threshold);
//        System.out.println("threshold2 "+threshold2);
        List<String> filteredSvcs = svcLocs.entrySet().stream()
                .filter(entry ->{
                    if (entry.getValue() < threshold && svcFiles.get(entry.getKey()) < threshold2){
                        nanoPaths.get("nano").add(entry.getKey());
                        return true;
                    }
                    else{
                        nanoPaths.get("normal").add(entry.getKey());
                        return false;
                    }
                   } )  // 过滤条件
                .map(Map.Entry::getKey)  // 提取 key
                .collect(Collectors.toList());  // 收集成 List

        return nanoPaths;
    }



    public List<Set<String>>  detectCS(List<SvcTransRes> svcTransResList){
        int callNum = 0;
        int chainNum = 0;
        List<Set<String>> result = new ArrayList<>();
        for(SvcTransRes svcTransRes:  svcTransResList){
            List<RequestChain>  requestChainList = svcTransRes.getRequestChainList();
            chainNum += requestChainList.size();
            for (RequestChain requestChain:requestChainList){
                callNum +=requestChain.getChain().getSubNodes().size();
            }
        }
        TraceDetail traceDetail = TraceUtils.getCountsPerSvcPR();
        double avgCallPR = traceDetail.getAvgcallPR();
        double threshold = avgCallPR + 0.5* avgCallPR;
        Map<String, Map<String, Integer>> callNumMapPR = traceDetail.getCallNumMapPR();
        for (String service: callNumMapPR.keySet()){
            Map<String, Integer> callMap= callNumMapPR.get(service);
            for (String called: callMap.keySet()){
                if (callMap.get(called) > threshold){
                    if(!result.stream()
                            .anyMatch(set -> set.contains(service) && set.contains(called))){
                        Set<String> callSet =new LinkedHashSet<>();
                        callSet.add(service);
                        callSet.add(called);
                        result.add(callSet);
                    }
                }
            }
        }
        return result;

    }

    public Map<String, Map<String,UrlItem>> detectNAV(List<String> servicePaths, Map<String, String> nameMap) throws IOException, ParseException {
        Map<String, Map<String,UrlItem>> navDetails = new HashMap<>();
        ApiVersionContext apiVersionContext = new ApiVersionContext();
        ApiParserUtils apiParserUtils = new ApiParserUtils();
        for (String service: servicePaths){
            String serviceName = FileFactory.getServiceName(service);
            apiVersionContext.getUnversionedMap().put(serviceName, new HashMap<>());
            apiVersionContext.getMissingUrlMap().put(serviceName,new HashMap<>());
            List<String> javaFiles = FileFactory.getJavaFiles(service);
            for (String javafile : javaFiles) {
                if(FileFinder.isController(javafile)) {
                    File file = new File(javafile);
                    apiParserUtils.inspectJavaFile(file, apiVersionContext, serviceName, navDetails);
                }
            }
        }
        return navDetails;


    }

    public Map<List<String>, Integer>  detectSC(List<SvcTransRes> svcTransResList){
        Set<String> serviceNames = new LinkedHashSet<>(); // 使用 LinkedHashSet
        Map<List<String>, Integer> chainMap = new LinkedHashMap<>(); // 使用 LinkedHashMap
        for(SvcTransRes svcTransRes:  svcTransResList){
            List<RequestChain>  requestChainList = svcTransRes.getRequestChainList();
            for (RequestChain requestChain:requestChainList){
                TraceChainNode firstNode = requestChain.getChain();
                firstNode.collectServiceNames(0,serviceNames, chainMap);
            }
        }
        int totalCount = 0;
        int totalValues = 0;
        for (Integer value : chainMap.values()) {
            totalCount += value; // 累加所有的 value
            totalValues++; // 计算条目数量
        }
        double avgChainLength =(double) totalCount / totalValues;
        double threshold = 1.5 * avgChainLength;
        // 存储符合条件的 Set
        // 使用 Map<Set<String>, Integer> 来存储符合条件的 Set 及其计数
        Map<List<String>, Integer> filteredMap = new HashMap<>();

// 遍历 chainSet
        for (Map.Entry<List<String>, Integer> entry : chainMap.entrySet()) {
            List<String> currentList = entry.getKey();
            int currentValue = entry.getValue();

            // 检查当前 value 是否大于平均值的 1.5 倍
            if (currentValue > threshold) {
                // 检查是否与已有 Set 完全一致
                boolean isExactMatch = false;
                int existingCount = 0;
                // 用于跟踪是否要替换已有集合
                for (Map.Entry<List<String>, Integer> existingEntry : filteredMap.entrySet()) {
                    List<String> existingList = existingEntry.getKey();

                    // 检查是否完全一致
                    if (existingList.equals(currentList)) {
                        isExactMatch = true;
                        existingCount = existingEntry.getValue(); // 记录当前计数
                        break;
                    }
                    // 检查是否为子集
                    if (existingList.size() < currentList.size() && currentList.containsAll(existingList)) {
                        filteredMap.remove(existingList);
                    }
                }

                // 如果是完全一致，则更新计数
                if (isExactMatch) {
                    filteredMap.put(currentList, existingCount + 1);
                } else {
                    // 如果不是子集且不是完全一致，则添加到 filteredSets 中
                    boolean isSubset = false;

                    for (List<String> existingList : filteredMap.keySet()) {
                        // 通过迭代器检查顺序
                        Iterator<String> currentIterator = currentList.iterator();
                        Iterator<String> existingIterator = existingList.iterator();

                        String currentElement = currentIterator.hasNext() ? currentIterator.next() : null;

                        while (existingIterator.hasNext()) {
                            String existingElement = existingIterator.next();
                            // 如果当前元素与已有集合的元素匹配
                            if (currentElement != null && currentElement.equals(existingElement)) {
                                currentElement = currentIterator.hasNext() ? currentIterator.next() : null;
                            }
                            // 如果 currentSet 中的元素已经没有了，说明是子集
                            if (currentElement == null) {
                                break;
                            }
                        }

                        // 如果 currentSet 中的所有元素都被匹配，说明是子集
                        if (currentElement == null) {
                            isSubset = true;
                            break;
                        }
                    }

                    // 如果不是子集，则添加到 filteredSets 中
                    if (!isSubset) {
                        filteredMap.put(currentList, 1); // 新增条目，计数为 1
                    }
                }
            }
        }
        // 按照值降序排序
        List<Map.Entry<List<String>, Integer>> sortedEntries = filteredMap.entrySet()
                .stream()
                .sorted(Map.Entry.<List<String>, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        // 创建一个新的 LinkedHashMap 以保持插入顺序
        Map<List<String>, Integer> sortedFilteredMap = new LinkedHashMap<>();

        // 将排序后的结果赋值给新的 Map
        for (Map.Entry<List<String>, Integer> entry : sortedEntries) {
            sortedFilteredMap.put(entry.getKey(), entry.getValue());
        }

        // 如果需要，可以将 sortedFilteredMap 赋值回 filteredMap
        filteredMap = sortedFilteredMap;
        // 记录存在service chain异味的完整请求链的出现次数（不包括子链），
        return filteredMap;
    }



    public List<Set<String>> detectSP(String projectPath,  Map<String, String> namePathMap) throws IOException {
        HttpHeaders httpHeaders = new HttpHeaders();
        List<Set<String>> servicesPath =new ArrayList<>();

        SharedDatabaseContext sharedDatabaseContext = sharedDatabaseAndServiceIntimacyService.getsharedDatabaseandServiceIntimacy(projectPath);
        Map<String, ArrayList<String>> spMap = sharedDatabaseContext.getSharedDatabaseMap();
        if(sharedDatabaseContext.getSharedDatabaseMap().isEmpty())
            return null;
        else{
            this.sharedDatabaseContext = sharedDatabaseContext;
            for (Map.Entry<String, ArrayList<String>> entry : spMap.entrySet()) {
                Set<String> valueSet = new HashSet<>();
                List<String> nameList = entry.getValue();
                for (String serviceName: nameList){
                    valueSet.add(namePathMap.get(serviceName));
                }
                servicesPath.add(valueSet);
            }
            return servicesPath;
        }
    }

    public Map<String, String> detectNSDP(String projectPath, Map<String, String> filePathToMicroserviceName) throws XmlPullParserException, IOException {
        Map<String, String> result = new LinkedHashMap<>();
        for (String filePath: filePathToMicroserviceName.keySet()) { // 遍历每个模块
            List<Dependency> dependencies = MavenParserUtils.getMavenDependencies(new File(filePath + "/pom.xml").getAbsolutePath());
            result.put(filePath, "");
            for (Dependency dependency: dependencies) {
                if (dependency.getArtifactId().contains("eureka-server")) {
                    result.put(filePath, "eureka-server");
                } else if (dependency.getArtifactId().contains("eureka-client")) {
                    result.put(filePath, "eureka-client");
                } else if (dependency.getArtifactId().contains("nacos")) {
                    result.put(filePath, "nacos");
                }
            }
        }
        return result;
    }

    public int detectedNAG(String projectPath, Map<String, String> filePathToMicroserviceName) throws IOException, XmlPullParserException {
        int result = 0;
        for (String filePath: filePathToMicroserviceName.keySet()) {
            List<String> pomXmlPaths = FileFactory.getPomXmlPaths(filePath);
            for (String pomXmlPath: pomXmlPaths) {
                List<Dependency> dependencies = MavenParserUtils.getMavenDependencies(pomXmlPath);
                for (Dependency dependency: dependencies) {
                    if (dependency.getArtifactId().contains("gateway")) {
                        result += 1;
                        Map<String, Object> configurations = YamlAndPropertiesParserUtils.getConfigurations(filePath);
                        for (String key: configurations.keySet()) {
                            if (key.startsWith("spring.cloud.gateway")) {
                                result += 1;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public Map<String, String> detectedUS(String projectPath, Map<String, String> filePathToMicroserviceName) throws IOException, XmlPullParserException {
        Map<String, String> result = new LinkedHashMap<>();
        for (String filePath: filePathToMicroserviceName.keySet()) {
            List<String> pomXmlPaths = FileFactory.getPomXmlPaths(filePath);
            for (String pomXmlPath: pomXmlPaths) {
                List<Dependency> dependencies = MavenParserUtils.getMavenDependencies(pomXmlPath);
                for (Dependency dependency: dependencies) {
                    if (dependency.getArtifactId().equals(dependenciesConfig.getConfigServer().getArtifactId())) {
                        result.put(filePath, "config-server");
                    } else if (dependency.getArtifactId().equals(dependenciesConfig.getConfigClient().getArtifactId())) {
                        result.put(filePath, "config-client");
                    }
                }
            }
        }
        return result;
    }

    /**
     * 检测 EBSI
     * @param projectPath 系统路径
     * @param filePathToMicroserviceName 微服务模块路径与名称的映射
     * @return 检测结果 key 为微服务名称 value 为存在硬编码的 Java 文件列表
     * @throws IOException
     */
    public Map<String, List<String>> detectedEBSI(String projectPath, Map<String, String> filePathToMicroserviceName) throws IOException {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String currentFilePath: filePathToMicroserviceName.keySet()) {
            List<String> currentJavaFiles = FileFactory.getJavaFiles(currentFilePath);
            for (String javaFile: currentJavaFiles) {
                if (JavaParserUtils.containsEndPointBasedInteraction(javaFile)) {
                    if (!result.containsKey(currentFilePath)) {
                        result.put(currentFilePath, new ArrayList<>());
                    }
                    result.get(currentFilePath).add(javaFile);
                }
            }
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        String directoryPath = "D:\\code\\demo-collection\\Service-Demo\\routeservice";

        // 构造 cloc 命令
        ProcessBuilder processBuilder = new ProcessBuilder("cloc", directoryPath);
        processBuilder.redirectErrorStream(true);

        // 执行命令
        Process process = processBuilder.start();

        // 输出命令执行结果并查找代码行数
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        int codeLines = 0;

        while ((line = reader.readLine()) != null) {
            // 解析 cloc 输出，查找包含代码行数的行
            if (line.contains("SUM:")) {
                // 例如输出行：SUM: 10 1000 1000 10000
                String[] parts = line.split("\\s+"); // 根据空格分割
                if (parts.length >= 5) {
                    // 代码行数是第三个字段（假设输出格式没有变化）
                    try {
                        codeLines = Integer.parseInt(parts[4]);
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing code lines");
                    }
                }
            }
        }

        // 等待命令执行完成
        process.waitFor();

        // 输出代码行数
        System.out.println("Total code lines: " + codeLines);
    }
}
