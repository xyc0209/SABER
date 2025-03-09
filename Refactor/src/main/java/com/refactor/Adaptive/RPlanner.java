package com.refactor.Adaptive;


import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.refactor.chain.analyzer.layer.LayerType;
import com.refactor.chain.utils.Node;
import com.refactor.config.DependenciesConfig;
import com.refactor.config.GatewayConfig;
import com.refactor.context.SystemMavenInfo;
import com.refactor.detail.ModificationRecorder;
import com.refactor.detail.model.CodeModification;
import com.refactor.dto.ISIInfo;
import com.refactor.dto.SvcCallDetail;
import com.refactor.enumeration.ModificationType;
import com.refactor.enumeration.RefactorType;
import com.refactor.enumeration.TemplateFile;
import com.refactor.suggest.RefactorISISuggestion;
import com.refactor.suggest.RefactorSPSuggestion;

import com.refactor.dto.SPDetail;
import com.refactor.dto.SharedServicesInfo;
import com.refactor.suggest.RefactorSuggestion;
import com.refactor.trace.SvcTransRes;
import com.refactor.utils.*;
import com.refactor.utils.cohesion.CohesionAnalyzer;
import com.refactor.utils.nano.FitnessCalculator;
import com.refactor.utils.nano.NanoService;
import com.refactor.utils.nano.ServiceDetail;
import com.refactor.utils.nano.ServiceMerger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
@Component
public class RPlanner {
    private static int Threshold = 2;   //2，直接拆分; >=3,不拆分，权利交给开发人员，只给建议


    public FileFactory fileFactory;
    public DatabaseUtils databaseUtils;
    @Autowired
    public RAnalyser rAnalyser;

    @Autowired
    public RefactorSuggestion refactorSuggestion;

    @Autowired
    private DependenciesConfig dependenciesConfig;

    private static final String CONFIG_FOLDER_NAME = "config";
    public HashMap<String, SvcCallDetail> serviceCallDetails;

    private static final String AUTO_CONFIG_FILE_NAME = "MyLoadBalancer.java";

    public RPlanner() {
        fileFactory = new FileFactory();
        databaseUtils = new DatabaseUtils();
    }


    public Map<String, Map<String, String>> planSP(List<Set<String>> spList,  Map<String, List<String>> svcEntityMap) throws IOException {
        Map<String, Map<String, String>> serviceModifiedDetails = new HashMap<>();
        List<RefactorSPSuggestion> refactorSuggestions = new ArrayList<>();
        List<SharedServicesInfo> sharedServicesInfoList = new ArrayList<>();
        for(Set<String> spSet: spList){
            // 获取 spSet 中的所有服务名称
            String[] servicesArray = spSet.toArray(new String[0]);
            List<SPDetail> spDetails = new ArrayList<>();
            // 遍历服务的组合，寻找公共实体
            for (int i = 0; i < servicesArray.length; i++) {
                for (int j = i + 1; j < servicesArray.length; j++) {
                    List<String> commonEntities =new ArrayList<>();
                    String serviceName1 = servicesArray[i];
                    String serviceName2 = servicesArray[j];

                    // 获取服务名称
                    String name1 = fileFactory.getServiceName(serviceName1);
                    String name2 = fileFactory.getServiceName(serviceName2);

                    // 获取实体列表
                    List<String> entityList1 = svcEntityMap.get(name1);
                    List<String> entityList2 = svcEntityMap.get(name2);

                    // 计算公共实体，如果两个服务的实体列表都存在
                    if (entityList1 != null && entityList2 != null) {
                        Set<String> set1 = new HashSet<>(entityList1);
                        Set<String> set2 = new HashSet<>(entityList2);

                        // 取交集
                        set1.retainAll(set2);

                        // 将找到的公共实体添加到 commonEntities 集合中
                        if (!set1.isEmpty()){
                            sharedServicesInfoList.add(new SharedServicesInfo(name1, name2, set1));
                            String suggestion = String.format("It is recommended that the entities shared between services %s and %s be accessed via HTTP requests for data interaction, to avoid using a shared database.",
                                    name1, name2);
                            refactorSuggestions.add(new RefactorSPSuggestion("Shared Database", new HashSet<>(Arrays.asList(name1, name2)), suggestion));
                        }

                    }
                }
            }
            //处理 ISI建议
            Map<String, ArrayList<String>> serviceIntimacyMap = rAnalyser.sharedDatabaseContext.getServiceIntimacyMap();
            Map<String, ArrayList<String>>  servicecDatabasesMap = rAnalyser.sharedDatabaseContext.getServicecDatabasesMap();
            if(!serviceIntimacyMap.isEmpty()){
                List<RefactorISISuggestion> refactorISISuggestions = new ArrayList<>();
                for (Map.Entry<String, ArrayList<String>> entry:  serviceIntimacyMap.entrySet()){
                    List<String> isiList = entry.getValue();
                    List<String> serviceWithSingleBase = new ArrayList<>();
                    List<String> serviceWithMultipleBases = new ArrayList<>();
                    for(String service: isiList){
                        if (servicecDatabasesMap.get(service).size() == 1)
                            serviceWithSingleBase.add(service);
                        else
                            serviceWithMultipleBases.add(service);
                    }
                    String databaseURL = entry.getKey();
                    if (isiList.size() > 1){

                    }
                    String singleNames = String.join(", ", serviceWithSingleBase); // 使用", "作为分隔符
                    String multipleNames = String.join(", ", serviceWithMultipleBases); // 使用", "作为分隔符

                    // 生成新的建议内容
                    String suggestion = String.format("There are inappropriate service intimacies between services (%s) and (%s), where services (%s) access the private data of services (%s). It is recommended to access the data through HTTP requests instead of directly accessing the private data of other services.",
                            multipleNames, singleNames, multipleNames, singleNames);
                    refactorISISuggestions.add(new RefactorISISuggestion(databaseURL, new HashSet<>(isiList), suggestion));
                }
                refactorSuggestion.setRefactorISISuggestions(refactorISISuggestions);
            }

            if (!sharedServicesInfoList.isEmpty()){
                //数据耦合，给出重构建议
                refactorSuggestion.setRefactorSPSuggestions(refactorSuggestions);
                return null;
            }
            else{int index = 0;
                //非耦合，则创建新数据库，复制必要的表结构和数据，编译，打包，制作镜像，部署，delete实例
                for (String servicePath: spSet){
                    String serviceName = fileFactory.getServiceName(servicePath);
                    List<String> entityList = svcEntityMap.get(serviceName);
                    String application = "";

                    List<String> applicationYamlOrProperties= fileFactory.getApplicationYamlOrPropertities(servicePath);
//                    if(applicationYamlOrProperties.size() == 1)
//                        application = applicationYamlOrProperties.get(0);

                    Map<String,String> databaseDetails = databaseUtils.getDatabase(applicationYamlOrProperties);
                    String sourceDatabaseName = databaseDetails.get("sourceDatabaseName");
                    String newDatabaseName = null;
                    if (index != 0) {
                        //则创建新数据库，复制list中的entity对应的的表结构和数据
                        newDatabaseName = sourceDatabaseName + "_sp" + String.valueOf(index);
                        DatabaseUtils.splitDatabase(databaseDetails.get("sourceDatabaseUrl"), databaseDetails.get("username"), databaseDetails.get("password"), sourceDatabaseName, newDatabaseName, entityList);
                        //修改yaml文件
                        FileFactory.setSVCNameAndDBName(servicePath, serviceName, newDatabaseName);

                        serviceModifiedDetails.put(servicePath, FileFactory.getServiceDetails(servicePath));
                    }
                }
            }
        }
        return serviceModifiedDetails;
    }

    public Map<String, Map<String, String>> planCS(List<Set<String>>  resultCSCall, Map<String, String> namePathMap) throws IOException, XmlPullParserException {
        Map<String, Map<String, String>> serviceModifiedDetails = new HashMap<>();
        //获得所有不同的set集合
        Set<String> csSet = new HashSet<>();
        Map<String,Set<String>> svc_csMap = new HashMap<>();
        for(Set<String> set: resultCSCall){
            for (String svc: set){
                if(!csSet.contains(svc)){
                    csSet.add(svc);
                    svc_csMap.put(svc, set);
                }
            }
        }
        //遍历每个servicepath，修改源码添加配置类，  serviceName
        for (String svc: csSet) {
            String servicePath = namePathMap.get(svc);
            List<String> applicationPath = FileFactory.getApplicationPath(servicePath);
            //添加配置类
            FileFactory.addConfig(applicationPath, svc_csMap.get(svc), svc);
            //添加依赖
            DependencyUtils.modifyPomFile(new File(FileFactory.getPomFiles(servicePath).get(0)));
            serviceModifiedDetails.put(servicePath, FileFactory.getServiceDetails(servicePath));
        }
        return serviceModifiedDetails;
    }
    public Map<String, Map<String, String>> planNAV(Map<String, Map<String, UrlItem>> navDetails, Map<String, String> namePathMap) throws XmlPullParserException, IOException {
        Map<String, Map<String, String>> serviceModifiedDetails = new HashMap<>();
        ApiParserUtils apiParserUtils = new ApiParserUtils();
        for (String serviceName: navDetails.keySet()){
            Map<String, UrlItem> urlDetails = navDetails.get(serviceName);
            String version = DependencyUtils.getVserion(namePathMap.get(serviceName));
            for (String controllerPath: urlDetails.keySet()){
                apiParserUtils.updateControllerFile(controllerPath, urlDetails.get(controllerPath), version);
            }
            String servicePath = namePathMap.get(serviceName);
            serviceModifiedDetails.put(servicePath, FileFactory.getServiceDetails(servicePath));
        }
        return serviceModifiedDetails;
    }
    public Map<String, Map<String, String>> planSC( Map<List<String>, Integer> resultMap, Map<String, String> namePathMap) throws IOException, XmlPullParserException {
        Map<String, Map<String, String>> serviceModifiedDetails = new HashMap<>();
        //记录每个服务的所有调用服务
        Map<String, Set<String>> callMap = new HashMap<>();

        // 遍历 resultMap
        for (List<String> path : resultMap.keySet()) {
            for (int i = 0; i < path.size() - 1; i++) {
                String callNode = path.get(i);
                String calledNode = path.get(i + 1);

                // 更新后继节点集
                callMap.putIfAbsent(callNode, new HashSet<>());
                callMap.get(callNode).add(calledNode);
            }
        }
        for(String svc: callMap.keySet()){
            String servicePath = namePathMap.get(svc);
            List<String> applicationPath = FileFactory.getApplicationPath(servicePath);
            //添加配置类
            FileFactory.addConfig(applicationPath, callMap.get(svc), svc);
            //添加依赖
            DependencyUtils.modifyPomFile(new File(FileFactory.getPomFiles(servicePath).get(0)));
            serviceModifiedDetails.put(servicePath, FileFactory.getServiceDetails(servicePath));
        }
        return serviceModifiedDetails;
    }

    public Map<String, Map<String, String>> planNano(String projectPath, List<SvcTransRes> svcTransResList, List<String> normalServicePaths, List<String> nanoServicePaths, Map<String, List<String>> svcEntityMap, Map<String, String> namePathMap) throws IOException {
        ParseResultUtils parseResultUtils = new ParseResultUtils();
        List<String> serviceModifiedPaths = new ArrayList<>(); //记录更改服务调用源码的的服务路径，后续用于打包
        Map<String, Map<String, String>> serviceModifiedDetails = new HashMap<>();
        this.serviceCallDetails = parseResultUtils.ESBUsageAnalysis(projectPath);
        List<String> serviceClassFiles = FileFinder.findServiceClasses(projectPath);
        TraceUtils.svcTransResList = svcTransResList;
        //初始化 normal
        List<ServiceDetail> normalServices = new ArrayList<>();
        Map<String, Double> cohesionMap = null;
        double maxCoupling = 0;
        for(String normalServicePath: normalServicePaths){
            cohesionMap = CohesionAnalyzer.getServiceCohesion(normalServicePath);
            String svcName = FileFactory.getServiceDetails(normalServicePath).get("serviceName");
            SvcCallDetail svcCallDetail = serviceCallDetails.get(svcName);
            double coupling = svcCallDetail.getCalledService().size() + svcCallDetail.getCallService().size();
            if (coupling > maxCoupling)
                maxCoupling = coupling;
            //获取服务调用次数，接收类型为List<SvcTransRes>, 1.遍历获得总次数allCount，2.分析nano服务合并到正常服务的次数targetCount(每个调用链中正常服务调用nano服务的情况), 3. allCount - targetCount为合并后的总次数

            ServiceDetail service1 = new ServiceDetail(svcName, normalServicePath, TraceUtils.getServiceCallCounts(svcName), coupling, (double)cohesionMap.get("cohesion"), cohesionMap);
//            ServiceDetail service1 = new ServiceDetail(svcName, normalServicePath, 1, coupling, (double)cohesionMap.get("cohesion"), cohesionMap);
            normalServices.add(service1);
        }
        //初始化 nano
        List<ServiceDetail> nanoServices = new ArrayList<>();
        for(String nanoServicePath: nanoServicePaths){
            cohesionMap = CohesionAnalyzer.getServiceCohesion(nanoServicePath);
            String svcName = FileFactory.getServiceDetails(nanoServicePath).get("serviceName");
            SvcCallDetail svcCallDetail = serviceCallDetails.get(svcName);
            double coupling = svcCallDetail.getCalledService().size() + svcCallDetail.getCallService().size();
            if (coupling > maxCoupling)
                maxCoupling = coupling;

//            ServiceDetail service1 = new NanoService(svcName, nanoServicePath, 1, coupling, (double)cohesionMap.get("cohesion"), cohesionMap);
            ServiceDetail service1 = new NanoService(svcName, nanoServicePath, TraceUtils.getServiceCallCounts(svcName), coupling, (double)cohesionMap.get("cohesion"), cohesionMap);
            nanoServices.add(service1);
        }

        // 存储当前分配
        List<Integer> allocation = new ArrayList<>();
        for (int i = 0; i < nanoServices.size(); i++) {
            allocation.add(0); // 初始化为0，表示未分配
        }

        // 开始递归分配
        ServiceMerger.serviceCallDetails = this.serviceCallDetails;
        double totalServiceRank = FitnessCalculator.calculateTotalServiceRank(nanoServices) + FitnessCalculator.calculateTotalServiceRank(normalServices);
        ServiceMerger.assignNanoServices(nanoServices, normalServices, allocation, totalServiceRank, maxCoupling, 0);
        System.out.println("最佳分配: " + ServiceMerger.bestAllocation + " -> 最佳适应度: " + ServiceMerger.bestFitness);
        System.out.println("merged service"+normalServices.get(ServiceMerger.bestAllocation.get(0)).getName());
        //文件合并，数据库内容合并
        for (int i=0; i < allocation.size(); i++){
            String nanoPath = nanoServicePaths.get(0);
            String normalPath = normalServicePaths.get(allocation.get(i));
            String nanoName = fileFactory.getServiceName(nanoPath);
            String normalName = fileFactory.getServiceName(normalPath);
            //将nanoPath路径下的内容合并到normalPath下，除了yaml文件和标有@SpringBootApplication注解的启动类文件外
            FileFactory.mergePaths(nanoPath, normalPath);
            //复制pom文件
            DependencyUtils.mergeDependencies(nanoPath, normalPath);
            //复制nanoPath路径下的application,yaml文件中指定的数据库中的内容到normalPath指定的数据库
            Map<String,String> nanoDatabaseDetails = databaseUtils.getDatabaseDetails(this.fileFactory,nanoPath);
            Map<String,String> normalDatabaseDetails = databaseUtils.getDatabaseDetails(this.fileFactory,normalPath);
            DatabaseUtils.copyDatabase(nanoDatabaseDetails, normalDatabaseDetails, svcEntityMap.get(nanoName));

            FileFactory.setSVCNameAndDBName(normalPath, normalName +"_" + nanoName, null);
            //修改调用

            ServiceReplaceUtils serviceReplaceUtils1 = new ServiceReplaceUtils(nanoName, normalName, null);
            for(String serviceClassFile: serviceClassFiles){
                if (serviceReplaceUtils1.serviceReplace(serviceClassFile)){
                    String addServicePath = FileFinder.trimPathBeforeSrc(serviceClassFile);
                    if (!serviceModifiedPaths.contains(addServicePath))
                        serviceModifiedDetails.put(addServicePath, ServiceReplaceUtils.getSVCModifiedDetails(FileFactory.getServiceDetails(addServicePath).get("serviceName"), FileFactory.getServiceDetails(addServicePath).get("servicePort")));
                }
            }
            String path1 = FileFinder.findCommonURLPrefix(nanoPath);
            Map<String, String> pathDetails = new HashMap<>();
            pathDetails.put(normalName, path1);
            String gatewayPath = FileFactory.gatewayServiceExisted(projectPath, normalName, pathDetails);
            if (gatewayPath != null) {
                Map<String, String> addSVCDetails = FileFactory.getServiceDetails(gatewayPath);
                serviceModifiedDetails.put(gatewayPath, ServiceReplaceUtils.getSVCModifiedDetails(addSVCDetails.get("serviceName"), addSVCDetails.get("servicePort")));
//                serviceModifiedPaths.add(FileFactory.gatewayServiceExisted(projectPath, svcName, svcName1, svcName2, path1, path2));
            }



        }

        return serviceModifiedDetails;
    }
    public Map<String, Map<String, String>> planGod(List<Node> nodes, String servicePath, int averageSize) throws IOException {
        LouvainAlgorithm algorithm = new LouvainAlgorithm(nodes);
        List<Community> communities = algorithm.detectCommunities();
        System.out.println("communities SIZE"+communities.size());
        for (Community community: communities)
            System.out.println("+++++++ : " +community.toString());
        List<String> serviceModifiedPaths = new ArrayList<>(); //记录更改服务调用源码的的服务路径，后续用于打包
        Map<String, Map<String, String>> serviceModifiedDetails = new HashMap<>();
//        for (Community community : communities) {
//            System.out.println("Community: " + community.getMembers().stream().map(n -> n.getId()).collect(Collectors.toList()));
//
//            for (Node node : community.getMembers()) {
//                if(node.getType() == LayerType.CONTROLLER) {
//
//                    for (Map.Entry<Node, Set<Node>> entry : node.dfsWithTwoOrMoreParents(node,community.getParentCountMap()).entrySet()) {
//                        Node key = entry.getKey();
//                        if (entry.getValue().size()>=2)
//                            System.out.println("Key: " + key.getId() + ", Size: " + entry.getValue().size());
//                    }
//                }
//            }
//            System.out.println(""+community.getParentCountMap().size());
//        }
        List<Community> mergedCommunities = new ArrayList<>();
        System.out.println("averageSize" +averageSize);
        // 遍历所有的 communities
        while (!communities.isEmpty()) {
            Community currentCommunity = communities.remove(0);
            List<Node> entityNodes = currentCommunity.getMembers().stream()
                    .filter(node -> node.getType() == LayerType.ENTITY)
                    .collect(Collectors.toList());

            long currentEntityCount = entityNodes.size();

            // 如果当前社区中的 ENTITY 数量小于 averageSize，则需要合并
            while (currentEntityCount < averageSize && !communities.isEmpty()) {
                // 获取下一个 community 并合并它的 ENTITY 节点
                Community nextCommunity = communities.remove(0);
                List<Node> nextEntityNodes = nextCommunity.getMembers().stream()
                        .filter(node -> node.getType() == LayerType.ENTITY)
                        .collect(Collectors.toList());

                // 合并两个社区的 ENTITY 节点
                currentCommunity.getMembers().addAll(nextCommunity.getMembers());
                currentEntityCount = currentEntityCount + nextEntityNodes.size();

                // 如果合并后仍未达到目标数量，继续合并
                if (currentEntityCount >= averageSize) {
                    break;
                }
            }

            // 将合并后的 community 添加到新的 mergedCommunities 列表中
            mergedCommunities.add(currentCommunity);
        }
        String application = "";

        List<String> applicationYamlOrProperties= fileFactory.getApplicationYamlOrPropertities(servicePath);
        Map<String,String> databaseDetails = databaseUtils.getDatabase(applicationYamlOrProperties);
        String sourceDatabaseName = databaseDetails.containsKey("sourceDatabaseName")? databaseDetails.get("sourceDatabaseName"): null;
        String svcName = FileFactory.getServiceDetails(servicePath).get("serviceName");
        String svcPort = FileFactory.getServiceDetails(servicePath).get("servicePort");
        String projectPath = fileFactory.getProjectPath(servicePath);
        List<String> serviceClassFiles = FileFinder.findServiceClasses(projectPath);
        Map<String, String> pathDetails = new HashMap<>();
        int size = mergedCommunities.size();
        System.out.println("mergedCommunities" +mergedCommunities.toString());
        for (Community community: mergedCommunities)
            System.out.println("community" +community.toString());
        //直接拆分
        if(size >= Threshold) {
            for (int i=0; i < size; i++){
                String newFolderName = servicePath +"_" + String.valueOf(i+1);
                fileFactory.copyFolder(servicePath, newFolderName);
                // Create a list of all nodeIds except for the current community
                List<Community> remainingCommunities = new ArrayList<>(mergedCommunities);
                remainingCommunities.remove(i); // Remove the current community

                // Get node IDs from the remaining communities
                List<String> nodeIds = new ArrayList<>();
                for (Community community : remainingCommunities) {
                    nodeIds.addAll(algorithm.getNodeIds(community));
                }

                // Delete files in the new folder based on the remaining node IDs
                fileFactory.deleteFilesInFolder(newFolderName, nodeIds);

                String newDatabaseName = null;
                if (sourceDatabaseName != null) {
                    newDatabaseName = sourceDatabaseName + "_split" + String.valueOf(i + 1);
                    DatabaseUtils.splitDatabase(databaseDetails.get("sourceDatabaseUrl"), databaseDetails.get("username"), databaseDetails.get("password"), sourceDatabaseName, newDatabaseName, null);
                }
                System.out.println("----SPLIT DATABASE SUCCESS----");
                String svcName1 = svcName + "-"+ String.valueOf(i+1);
                Map<String, String> details1 = ServiceReplaceUtils.getSVCModifiedDetails(svcName1, svcPort);
                if (i == 0)
                    FileFactory.setSVCNameAndDBName(newFolderName, svcName1, sourceDatabaseName);
                else
                    FileFactory.setSVCNameAndDBName(newFolderName, svcName1, newDatabaseName);
                //获得controller类上的requerst URL
                String path1 = FileFinder.findCommonURLPrefix(newFolderName);
                pathDetails.put(svcName1, path1);
                ServiceReplaceUtils.setPath(details1, path1);
                serviceModifiedDetails.put(newFolderName, details1);
                //修改调用源码
                ServiceReplaceUtils serviceReplaceUtils1 = new ServiceReplaceUtils(svcName, svcName1, Pattern.compile( ".*(" + java.util.regex.Pattern.quote(path1) + ".*)?"));
                for(String serviceClassFile: serviceClassFiles){
                    if (serviceReplaceUtils1.serviceReplace(serviceClassFile)){
                        String addServicePath = FileFinder.trimPathBeforeSrc(serviceClassFile);
                        if (!serviceModifiedPaths.contains(addServicePath))
                            serviceModifiedDetails.put(addServicePath, ServiceReplaceUtils.getSVCModifiedDetails(FileFactory.getServiceDetails(addServicePath).get("serviceName"), FileFactory.getServiceDetails(addServicePath).get("servicePort")));
//                        serviceModifiedPaths.add(addServicePath);
                    }
                }
            }

        // If spring cloud gateway service exist，update route rules,add path of gateway
            String gatewayPath = FileFactory.gatewayServiceExisted(projectPath, null, pathDetails);
            if (gatewayPath != null) {
                Map<String, String> addSVCDetails = FileFactory.getServiceDetails(gatewayPath);
                serviceModifiedDetails.put(gatewayPath, ServiceReplaceUtils.getSVCModifiedDetails(addSVCDetails.get("serviceName"), addSVCDetails.get("servicePort")));
//                serviceModifiedPaths.add(FileFactory.gatewayServiceExisted(projectPath, svcName, svcName1, svcName2, path1, path2));
            }
            return serviceModifiedDetails;
        }
        else
            return null;  //no change，only give suggestions
//        return serviceModifiedPaths;

    }

    public Map<String, Map<String, String>> planGodBefore(List<Node> nodes, String servicePath) throws IOException {
        LouvainAlgorithm algorithm = new LouvainAlgorithm(nodes);
        List<Community> communities = algorithm.detectCommunities();
        List<String> serviceModifiedPaths = new ArrayList<>(); //记录更改服务调用源码的的服务路径，后续用于打包
        Map<String, Map<String, String>> serviceModifiedDetails = new HashMap<>();
        for (Community community : communities) {
            System.out.println("Community: " + community.getMembers().stream().map(n -> n.getId()).collect(Collectors.toList()));

            for (Node node : community.getMembers()) {
                if(node.getType() == LayerType.CONTROLLER) {

                    for (Map.Entry<Node, Set<Node>> entry : node.dfsWithTwoOrMoreParents(node,community.getParentCountMap()).entrySet()) {
                        Node key = entry.getKey();
                        if (entry.getValue().size()>=2)
                            System.out.println("Key: " + key.getId() + ", Size: " + entry.getValue().size());
                    }
                }
            }
            System.out.println(""+community.getParentCountMap().size());
        }
        if(communities.size() >= Threshold) //直接拆分
        {
            String newFolderName1 = servicePath +"_1";
            String newFolderName2 = servicePath +"_2";
//            serviceModifiedPaths.add(newFolderName1);
//            serviceModifiedPaths.add(newFolderName2);
            fileFactory.copyFolder(servicePath, newFolderName1, newFolderName2);
            System.out.println("communities.get(0)"+communities.get(0));
            fileFactory.deleteFilesInFolder(newFolderName1,algorithm.getNodeIds(communities.get(0)));
            fileFactory.deleteFilesInFolder(newFolderName2,algorithm.getNodeIds(communities.get(1)));
            System.out.println("communities.get(0)"+communities.get(1));
            // split database
            String application = "";

            List<String> applicationYamlOrProperties= fileFactory.getApplicationYamlOrPropertities(servicePath);
//            if(applicationYamlOrProperties.size() == 1)
//                application = applicationYamlOrProperties.get(0);
            Map<String,String> databaseDetails = databaseUtils.getDatabase(applicationYamlOrProperties);
            String sourceDatabaseName = databaseDetails.get("sourceDatabaseName");
            String newDatabaseName = sourceDatabaseName + "_split";
            //拆分数据库
            DatabaseUtils.splitDatabase(databaseDetails.get("sourceDatabaseUrl"), databaseDetails.get("username"), databaseDetails.get("password"), sourceDatabaseName, newDatabaseName, null);
            System.out.println("----SPLIT DATABASE SUCCESS----");
            //更新yaml文件
            String svcName = FileFactory.getServiceDetails(servicePath).get("serviceName");
            String svcPort = FileFactory.getServiceDetails(servicePath).get("servicePort");
            String svcName1 = svcName + "-1";
            String svcName2 = svcName + "-2";
            Map<String, String> details1 = ServiceReplaceUtils.getSVCModifiedDetails(svcName1, svcPort);
            Map<String, String> details2 = ServiceReplaceUtils.getSVCModifiedDetails(svcName2, svcPort);


            FileFactory.setSVCNameAndDBName(newFolderName1, svcName1, sourceDatabaseName);
            FileFactory.setSVCNameAndDBName(newFolderName2, svcName2, newDatabaseName);

            //获得controller类上的requerst URL
            String path1 = FileFinder.findCommonURLPrefix(newFolderName1);
            String path2 = FileFinder.findCommonURLPrefix(newFolderName2);
            ServiceReplaceUtils.setPath(details1, path1);
            ServiceReplaceUtils.setPath(details2, path2);
            serviceModifiedDetails.put(newFolderName1, details1);
            serviceModifiedDetails.put(newFolderName2, details2);
            //修改调用源码
//            ServiceReplaceUtils serviceReplaceUtils1 = new ServiceReplaceUtils(svcName, svcName1, Pattern.compile(".*(/api/v1/.*)?"));
            ServiceReplaceUtils serviceReplaceUtils1 = new ServiceReplaceUtils(svcName, svcName1, Pattern.compile( ".*(" + java.util.regex.Pattern.quote(path1) + ".*)?"));
            ServiceReplaceUtils serviceReplaceUtils2 = new ServiceReplaceUtils(svcName, svcName2, Pattern.compile(".*(" + java.util.regex.Pattern.quote(path2) + ".*)?"));
            String projectPath = fileFactory.getProjectPath(servicePath);
            List<String> serviceClassFiles = FileFinder.findServiceClasses(projectPath);
            for(String serviceClassFile: serviceClassFiles){
                if (serviceReplaceUtils1.serviceReplace(serviceClassFile) || serviceReplaceUtils2.serviceReplace(serviceClassFile)){
                    String addServicePath = FileFinder.trimPathBeforeSrc(serviceClassFile);
                    if (!serviceModifiedPaths.contains(addServicePath))
                        serviceModifiedDetails.put(addServicePath, ServiceReplaceUtils.getSVCModifiedDetails(FileFactory.getServiceDetails(addServicePath).get("serviceName"), FileFactory.getServiceDetails(addServicePath).get("servicePort")));
//                        serviceModifiedPaths.add(addServicePath);
                }
            }
            //如果存在spring cloud gateway网关服务，则更新路由,添加网关服务路径
            String gatewayPath = FileFactory.gatewayServiceExistedBefore(projectPath, svcName, svcName1, svcName2, path1, path2);
            if (gatewayPath != null) {
                Map<String, String> addSVCDetails = FileFactory.getServiceDetails(gatewayPath);
                serviceModifiedDetails.put(gatewayPath, ServiceReplaceUtils.getSVCModifiedDetails(addSVCDetails.get("serviceName"), addSVCDetails.get("servicePort")));
//                serviceModifiedPaths.add(FileFactory.gatewayServiceExisted(projectPath, svcName, svcName1, svcName2, path1, path2));
            }
        }
//        return serviceModifiedPaths;
        return serviceModifiedDetails;
    }

    public Map<String, Object> planNSDP(String projectPath, Map<String, String> serviceDetails) throws XmlPullParserException, IOException {
        // 获取系统 pom 信息
        SystemMavenInfo systemMavenInfo = MavenParserUtils.getSystemMavenInfo(projectPath);
        ModificationRecorder recorder = new ModificationRecorder(systemMavenInfo, RefactorType.No_Service_Discovery_Pattern);
        Map<String, Object> modificationInfo = new HashMap<>();
        // 标识服务发现组件类型
        String discovery = "nacos";
        // 记录是否含有 eureka-server
        String eurekaServerPath = "";
        for (String filePath: serviceDetails.keySet()) {
            String d = serviceDetails.get(filePath);
            if (d.contains("nacos")) {
                discovery = "nacos";
            } else if (d.contains("eureka")) {
                discovery = "eureka";
                if (d.contains("eureka-server")) { // 记录 eureka-server
                    eurekaServerPath = filePath;
                }
            }
        }
        Map<String, Map<String, String>> serviceModifiedDetails = new HashMap<>();
        Map<String, String> filePathToMicroserviceName = FileFactory.getFilePathToMicroserviceName(projectPath);
        // 检测是否添加 Spring Cloud 依赖
        int hasSpringCloud = MavenParserUtils.hasSpringCloud(projectPath);
        if (hasSpringCloud == 1) {
            MavenParserUtils.addMavenDependencies(projectPath + File.separator + "pom.xml", dependenciesConfig.getSpringCloud());
            recorder.addRecord(projectPath,
                    new CodeModification(projectPath + File.separator + "pom.xml", ModificationType.DEPENDENCY_ADD,
                            "", "org.springframework.cloud:spring-cloud-dependencies", "添加 Spring Cloud 依赖"));
        } else if (hasSpringCloud == 2) {
            for (String filePath: serviceDetails.keySet()) {
                if (MavenParserUtils.hasSpringCloud(filePath) == 1) {
                    MavenParserUtils.addMavenDependencies(filePath + File.separator + "pom.xml", dependenciesConfig.getSpringCloud());
                    recorder.addRecord(filePathToMicroserviceName.get(filePath),
                            new CodeModification(filePath + File.separator + "pom.xml", ModificationType.DEPENDENCY_ADD,
                                    "", "org.springframework.cloud:spring-cloud-dependencies", "添加 Spring Cloud 依赖"));
                }
            }
        }
        if ("eureka".equals(discovery)) { // 使用的是 eureka
            if (eurekaServerPath.isEmpty()) { // 不存在 eureka-server
                // 获取 Eureka 服务端模板
                SpringBootProjectDownloaderUtils.downloadProject("eureka-server",
                        systemMavenInfo.getGroupId(),
                        "eureka-server", systemMavenInfo.getGroupId() + "." + "eurekaserver", Collections.singletonList("web"), projectPath);
                MavenParserUtils.addModule(projectPath, "eureka-server");
                recorder.addRecord(projectPath,
                        new CodeModification(projectPath, ModificationType.MODULE_ADD,
                                "", "eureka-server", "添加 Eureka 服务端模块"));
                String eurekaServerPomXml = new File(projectPath + "/eureka-server/pom.xml").getAbsolutePath();
                eurekaServerPath = new File(projectPath + "/eureka-server").getAbsolutePath();
                // 添加服务端依赖
                MavenParserUtils.addMavenDependencies(eurekaServerPomXml, dependenciesConfig.getEurekaServer());
                // 指定父 pom 信息
                MavenParserUtils.setParent(eurekaServerPomXml, systemMavenInfo.getGroupId(), systemMavenInfo.getArtifactId(), systemMavenInfo.getVersion());
                // 启动类添加注解
                String applicationJavaPath = new File(projectPath + "/eureka-server/src/main/java/" + systemMavenInfo.getGroupId().replace('.', '/') + "/eurekaserver" + "/EurekaServerApplication.java").getAbsolutePath();
                JavaParserUtils.addAnnotation(applicationJavaPath, "@EnableEurekaServer", "org.springframework.cloud.netflix.eureka.server.EnableEurekaServer");
                // 服务端配置
                String eurekaServerYamlPath = new File(projectPath + "/eureka-server/src/main/resources/application.properties").getAbsolutePath();
                FileFactory.updateApplicationYamlOrProperties(eurekaServerYamlPath, TemplateFile.EUREKA_SERVER);
            }
            // 获取 eureka-server 的 url
            Map<String, Object> eurekaServerConfigurations = YamlAndPropertiesParserUtils.getConfigurations(eurekaServerPath);
            Map<String, Object> eurekaClientConfigurations = YamlAndPropertiesParserUtils.transStringToMap("eureka.client.service-url.defaultZone", eurekaServerConfigurations.getOrDefault("eureka.client.service-url.defaultZone", 8888));
            String eurekaServerPomXml = new File(eurekaServerPath + "/pom.xml").getAbsolutePath();
            // 为其它模块添加 Eureka 客户端依赖和配置
            String parentPomXml = projectPath + File.separator + "pom.xml";
            for (String pomXmlPath: FileFactory.getPomXmlPaths(projectPath)) {
                String microserviceName = filePathToMicroserviceName.get(new File(pomXmlPath).getParent());
                if (!pomXmlPath.equals(parentPomXml) && !pomXmlPath.equals(eurekaServerPomXml)) { // 非父 pom 文件并且非 eureka-server pom 文件
                    MavenParserUtils.addMavenDependencies(pomXmlPath, dependenciesConfig.getEurekaClient());
                    recorder.addRecord(microserviceName,
                            new CodeModification(pomXmlPath, ModificationType.DEPENDENCY_ADD,
                                    "", "org.springframework.cloud:spring-cloud-starter-netflix-eureka-client", "添加 Eureka 客户端依赖"));
                }
            }
            for (String applicationYamlOrPropertiesPath: FileFactory.getApplicationYamlOrPropertiesPaths(projectPath)) {
                if (!applicationYamlOrPropertiesPath.contains(eurekaServerPath)) { // 非 eureka-server 配置文件
                    FileFactory.updateApplicationYamlOrProperties(applicationYamlOrPropertiesPath, TemplateFile.EUREKA_CLIENT);
                    FileFactory.updateApplicationYamlOrProperties(applicationYamlOrPropertiesPath, eurekaClientConfigurations);
                    recorder.addRecord(FileFactory.getMicroserviceName(applicationYamlOrPropertiesPath),
                            new CodeModification(applicationYamlOrPropertiesPath, ModificationType.CONFIG_UPDATE,
                                    "", TemplateFile.EUREKA_CLIENT.toString(), "添加 Eureka 客户端配置"));
                }
            }
        } else if ("nacos".equals(discovery)) {
            // 父 pom 添加 Spring Cloud Alibaba 依赖
            if (hasSpringCloud != 2) {
                MavenParserUtils.addMavenDependencies(projectPath + File.separator + "pom.xml", dependenciesConfig.getSpringCloudAlibaba());
                recorder.addRecord(projectPath,
                        new CodeModification(projectPath, ModificationType.DEPENDENCY_ADD, "", "org.springframework.cloud:spring-cloud-alibaba-dependencies", "添加 Spring Cloud Alibaba 依赖"));
            } else {
                // 其余模块添加 nacos 依赖
                for (String pomXmlPath: FileFactory.getPomXmlPaths(projectPath)) {
                    if (!pomXmlPath.equals(projectPath + File.separator + "pom.xml")) {
                        String microserviceName = filePathToMicroserviceName.get(new File(pomXmlPath).getParent());
                        MavenParserUtils.addMavenDependencies(pomXmlPath, dependenciesConfig.getSpringCloudAlibaba());
                        MavenParserUtils.addMavenDependencies(pomXmlPath, dependenciesConfig.getNacos());
                        recorder.addRecord(microserviceName,
                                new CodeModification(pomXmlPath, ModificationType.DEPENDENCY_ADD,
                                        "", "org.springframework.cloud:spring-cloud-alibaba-dependencies", "添加 Spring Cloud Alibaba 依赖"));
                        recorder.addRecord(microserviceName,
                                new CodeModification(pomXmlPath, ModificationType.DEPENDENCY_ADD,
                                        "", "com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery", "添加 Nacos 依赖"));
                    }
                }
            }
            // 其余模块更新 nacos 配置
            for (String applicationYamlOrPropertiesPath: FileFactory.getApplicationYamlOrPropertiesPaths(projectPath)) {
                FileFactory.updateApplicationYamlOrProperties(applicationYamlOrPropertiesPath, TemplateFile.NACOS_CLIENT);
                recorder.addRecord(FileFactory.getMicroserviceName(applicationYamlOrPropertiesPath),
                        new CodeModification(applicationYamlOrPropertiesPath, ModificationType.CONFIG_UPDATE, "", TemplateFile.NACOS_CLIENT.toString(), "添加 Nacos 配置"));
            }
        }
        for (String filePath : filePathToMicroserviceName.keySet()) {
            serviceModifiedDetails.put(filePathToMicroserviceName.get(filePath), FileFactory.getServiceDetails(filePath));
        }
        modificationInfo.put("modificationRecords", recorder.formatRecords());
        modificationInfo.put("serviceModifiedDetails", serviceModifiedDetails);
        return modificationInfo;
    }

    public Map<String, Object> planNAG(String projectPath, String discovery) throws Exception {
        Map<String, Object> modificationInfo = new HashMap<>();
        // 获取系统 pom 信息
        SystemMavenInfo systemMavenInfo = MavenParserUtils.getSystemMavenInfo(projectPath);
        ModificationRecorder recorder = new ModificationRecorder(systemMavenInfo, RefactorType.No_Api_Gateway);
        // 获取模板文件
        SpringBootProjectDownloaderUtils.downloadProject("gateway",
                systemMavenInfo.getGroupId(), "gateway", systemMavenInfo.getGroupId() + "." + "gateway",
                Collections.emptyList(), projectPath);
        Map<String, String> filePathToMicroserviceName = FileFactory.getFilePathToMicroserviceName(projectPath);
        // 模块添加
        MavenParserUtils.addModule(projectPath, "gateway");
        recorder.addRecord(projectPath,
                new CodeModification(projectPath, ModificationType.MODULE_ADD, null, "gateway", "Add gateway module"));
        // 新模块的 pom 文件路径
        String gatewayPomXml = projectPath + File.separator + "gateway" + File.separator + "pom.xml";
        // 设置父 pom 信息
        MavenParserUtils.setParent(gatewayPomXml, systemMavenInfo.getGroupId(), systemMavenInfo.getArtifactId(), systemMavenInfo.getVersion());
        // 将 properties 文件类型改为 yaml 类型
        String gatewayYamlPath = new File(projectPath + "/gateway/src/main/resources/application.properties").getAbsolutePath();
        FileFactory.deleteFile(gatewayYamlPath);
        gatewayYamlPath = FileFactory.createFile(new File(projectPath + "/gateway/src/main/resources/application.yaml").getAbsolutePath());
        MavenParserUtils.addMavenDependencies(gatewayPomXml, dependenciesConfig.getGateway());
        // 更改配置文件信息
        FileFactory.updateApplicationYamlOrProperties(gatewayYamlPath, TemplateFile.GATEWAY);
        if ("eureka".equals(discovery)) {
            MavenParserUtils.addMavenDependencies(gatewayPomXml, dependenciesConfig.getEurekaClient());
            recorder.addRecord("gateway",
                    new CodeModification(gatewayPomXml, ModificationType.DEPENDENCY_ADD, null, "org.springframework.cloud:spring-cloud-starter-netflix-eureka-client", "Add Eureka client dependency"));
        } else if ("nacos".equals(discovery)) {
            MavenParserUtils.addMavenDependencies(gatewayPomXml, dependenciesConfig.getNacos());
            recorder.addRecord("gateway",
                    new CodeModification(gatewayPomXml, ModificationType.DEPENDENCY_ADD, null, "com.alibaba.cloud:spring-cloud-starter-alibaba-nacos-discovery", "Add Nacos dependency"));
            YamlAndPropertiesParserUtils.updateApplicationYaml(gatewayYamlPath, TemplateFile.NACOS_CLIENT);
            recorder.addRecord("gateway",
                    new CodeModification(gatewayYamlPath, ModificationType.CONFIG_UPDATE, null, YamlAndPropertiesParserUtils.resolveSpecifiedTemplateYaml(TemplateFile.NACOS_CLIENT).toString(), "Add Nacos configuration"));
        }
        Map<String, Set<String>> microserviceNameToPreUrls = new LinkedHashMap<>();
        for (String filePath: filePathToMicroserviceName.keySet()) {
            String microserviceName = filePathToMicroserviceName.get(filePath);
            List<String> javaFiles = FileFactory.getJavaFiles(filePath);
            Set<String> preUrls = new LinkedHashSet<>();
            for (String javaFile: javaFiles) {
                Map<String, String> urls = JavaParserUtils.getMethodToApi(new File(javaFile));
                for (String url: urls.values()) {
                    if (url.length() > 1 && !url.startsWith("/{")) {
                        int second = url.indexOf('/', 1);
                        String preUrl = second == -1 ? url : url.substring(0, second);
                        preUrls.add(preUrl);
                    }
                }

            }
            if (!preUrls.isEmpty())
                microserviceNameToPreUrls.put(microserviceName, preUrls);
        }
        Map<String, Object> springCloudGateway = YamlAndPropertiesParserUtils.getSpringCloudGateway(microserviceNameToPreUrls);
        YamlAndPropertiesParserUtils.updateApplicationYaml(gatewayYamlPath, YamlAndPropertiesParserUtils.removeCustomClasses(springCloudGateway, GatewayConfig.class));
        recorder.addRecord("gateway",
                new CodeModification(gatewayYamlPath, ModificationType.CONFIG_UPDATE, null,
                        YamlAndPropertiesParserUtils.removeCustomClasses(springCloudGateway, GatewayConfig.class).toString(), "Add gateway configuration"));
        Map<String, Map<String, String>> serviceModifiedDetails = new HashMap<>();
        serviceModifiedDetails.put("gateway", FileFactory.getServiceDetails(projectPath + File.separator + "gateway"));
        modificationInfo.put("serviceModifiedDetails", serviceModifiedDetails);
        modificationInfo.put("modificationRecords", recorder.formatRecords());
        return modificationInfo;
    }

    public Map<String, Object> planUS(String projectPath, String discovery) throws IOException, XmlPullParserException {
        Map<String, Object> modificationInfo = new HashMap<>();
        Map<String, Map<String, String>> serviceModifiedDetails = new HashMap<>();
        SystemMavenInfo systemMavenInfo = MavenParserUtils.getSystemMavenInfo(projectPath);
        ModificationRecorder recorder = new ModificationRecorder(systemMavenInfo, RefactorType.Unnecessary_Settings);
        // 增加模块
        SpringBootProjectDownloaderUtils.downloadProject("config-server",
                systemMavenInfo.getGroupId(), "config-server", systemMavenInfo.getGroupId() + "." + "configserver",
                Collections.singletonList("web"), projectPath);
        String parentPomXml = projectPath + File.separator + "pom.xml";
        MavenParserUtils.addModule(projectPath, "config-server");
        recorder.addRecord("config-server",
                new CodeModification(projectPath,
                        ModificationType.MODULE_ADD, null, "config-server", "添加配置中心模块"));
        Map<String, String> filePathToMicroserviceName = FileFactory.getFilePathToMicroserviceName(projectPath);
        // 创建本地配置文件文件夹
        String shared = FileFactory.createDirectory(projectPath + "/config-server/src/main/resources/shared");
        // 配置父项目
        String configServerPomXml = new File(projectPath + "/config-server/pom.xml").getAbsolutePath();
        MavenParserUtils.setParent(configServerPomXml, systemMavenInfo.getGroupId(), systemMavenInfo.getArtifactId(), systemMavenInfo.getVersion());
        String configServerYamlPath = new File(projectPath + "/config-server/src/main/resources/application.properties").getAbsolutePath();
        FileFactory.deleteFile(configServerYamlPath);
        configServerYamlPath = FileFactory.createFile(new File(projectPath + "/config-server/src/main/resources/application.yaml").getAbsolutePath());
        YamlAndPropertiesParserUtils.updateApplicationYaml(configServerYamlPath, TemplateFile.CONFIG_SERVER);
        // 添加注解
        MavenParserUtils.addMavenDependencies(configServerPomXml, dependenciesConfig.getConfigServer());
        String applicationJavaPath = new File(projectPath + "/config-server/src/main/java/" + systemMavenInfo.getGroupId().replace('.', '/') + "/configserver" + "/ConfigServerApplication.java").getAbsolutePath();
        JavaParserUtils.addAnnotation(applicationJavaPath, "@EnableConfigServer", "org.springframework.cloud.config.server.EnableConfigServer");
        // 添加 Config client 依赖
        List<String> pomXmlPaths = FileFactory.getPomXmlPaths(projectPath);
        for (String pomXmlPath : pomXmlPaths) {
            if (!pomXmlPath.equals(parentPomXml) && !pomXmlPath.equals(configServerPomXml)) {
                String microserviceName = filePathToMicroserviceName.get(new File(pomXmlPath).getParent());
                MavenParserUtils.addMavenDependencies(pomXmlPath, dependenciesConfig.getConfigClient());
                MavenParserUtils.addMavenDependencies(pomXmlPath, dependenciesConfig.getBootstrap());
                recorder.addRecord(microserviceName,
                        new CodeModification(pomXmlPath, ModificationType.DEPENDENCY_ADD, null, "org.springframework.cloud:spring-cloud-config-client", "添加配置中心客户端依赖"));
                recorder.addRecord(microserviceName,
                        new CodeModification(pomXmlPath, ModificationType.DEPENDENCY_ADD, null, "org.springframework.cloud:spring-cloud-starter-bootstrap", "添加配置中心客户端依赖"));
            }
        }
        if ("eureka".equals(discovery)) {
            MavenParserUtils.addMavenDependencies(configServerPomXml, dependenciesConfig.getEurekaClient());
        } else if ("nacos".equals(discovery)) {
            MavenParserUtils.addMavenDependencies(configServerPomXml, dependenciesConfig.getNacos());
        }
        for (String filePath : filePathToMicroserviceName.keySet()) {
            String microserviceName = filePathToMicroserviceName.get(filePath);
            if (filePath.contains("eureka"))
                continue;
            // 创建 bootstrap.yml 文件
            String bootstrapYamlPath = FileFactory.createFile(filePath + "/src/main/resources/bootstrap.yml");
            if (bootstrapYamlPath != null) {
                YamlAndPropertiesParserUtils.updateApplicationYaml(bootstrapYamlPath, YamlAndPropertiesParserUtils.getConfigClient(microserviceName));
                YamlAndPropertiesParserUtils.updateApplicationYaml(bootstrapYamlPath, TemplateFile.CONFIG_CLIENT);
                String sharedYamlPath = FileFactory.createFile(shared + "/" + microserviceName + "-dev.yml");
                if (sharedYamlPath != null) {
                    Map<String, Object> configurations = YamlAndPropertiesParserUtils.getConfigurations(filePath);
                    YamlAndPropertiesParserUtils.updateApplicationYaml(sharedYamlPath, configurations);
                }
            }
            recorder.addRecord(filePathToMicroserviceName.get(filePath),
                    new CodeModification(bootstrapYamlPath, ModificationType.FILE_CREATE, null, "bootstrap.yml", "创建 bootstrap.yml 文件"));
        }
        for (String filePath : filePathToMicroserviceName.keySet()) {
            serviceModifiedDetails.put(filePathToMicroserviceName.get(filePath), FileFactory.getServiceDetails(filePath));
        }
        modificationInfo.put("serviceModifiedDetails", serviceModifiedDetails);
        modificationInfo.put("modificationRecords", recorder.formatRecords());
        return modificationInfo;
    }

    public Map<String, Object> planEBSI(String projectPath, Map<String, List<String>> detectedResult) throws IOException, XmlPullParserException {
        Map<String, Object> modificationInfo = new LinkedHashMap<>();
        ModificationRecorder recorder = new ModificationRecorder(MavenParserUtils.getSystemMavenInfo(projectPath), RefactorType.Endpoint_Based_Service_Interaction);
        Map<String, Map<String, String>> serviceModifiedDetails = new HashMap<>();
        Map<String, String> filePathToMicroserviceName = FileFactory.getFilePathToMicroserviceName(projectPath);
        Map<String, String> filePathToMicroservicePort = FileFactory.getFilePathToMicroservicePort(projectPath);
        Map<String, String> urlToMicroserviceName = new LinkedHashMap<>();
        for (String filePath: filePathToMicroserviceName.keySet()) {
            List<String> javaFiles = FileFactory.getJavaFiles(filePath);
            List<String> urls = new LinkedList<>();
            for (String javaFile: javaFiles) {
                Map<String, String> methodToApi = JavaParserUtils.getMethodToApi(new File(javaFile));
                for (String url: methodToApi.values()) {
                    urls.add(filePathToMicroservicePort.get(filePath) + url);
                }
            }
            for (String url: urls) {
                if (!url.isEmpty()) {
                    urlToMicroserviceName.put(url, filePathToMicroserviceName.get(filePath));
                }
            }
        }
        System.out.println(urlToMicroserviceName);
        for (String url: urlToMicroserviceName.keySet()) {
            String microserviceName = urlToMicroserviceName.get(url);
            for (String filePath: detectedResult.keySet()) {
                List<String> javaFiles = detectedResult.get(filePath);
                for (String javaFile : javaFiles) {
                    JavaParserUtils.restTemplateUrlReplacer(javaFile, microserviceName, Collections.singletonList(url), recorder);
                }
            }
        }
        for (String filePath : detectedResult.keySet()) {
            serviceModifiedDetails.put(filePathToMicroserviceName.get(filePath), FileFactory.getServiceDetails(filePath));
        }
        modificationInfo.put("serviceModifiedDetails", serviceModifiedDetails);
        modificationInfo.put("modificationRecords", recorder.formatRecords());
        return modificationInfo;
    }
}
