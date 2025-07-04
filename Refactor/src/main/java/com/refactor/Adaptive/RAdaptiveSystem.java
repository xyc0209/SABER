package com.refactor.Adaptive;

import com.github.javaparser.ParseException;
import com.google.protobuf.ServiceException;
import com.refactor.chain.analyzer.layer.LayerType;
import com.refactor.chain.utils.Node;
import com.refactor.dto.ChainKey;
import com.refactor.dto.ServiceDetail;
import com.refactor.service.RefactorServiceImpl;
import com.refactor.trace.SvcTransRes;
import com.refactor.utils.Community;
import com.refactor.utils.FileFactory;
import com.refactor.utils.LouvainAlgorithm;
import com.refactor.utils.UrlItem;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Component
public class RAdaptiveSystem {
    @Autowired
    private RMonitor monitor;
    @Autowired
    private RAnalyser analyser;
    @Autowired
    private RPlanner planner;
    @Autowired
    private RExecutor executor;



    public Map<String, Map<String, String>>  refactorMS(String projectPath) throws IOException, InterruptedException {
        Map<String, List<Node>>  callchain = this.monitor.getCommunities(projectPath);
        Map<String, String> namePathMap = FileFactory.getNamePathMap(this.monitor, projectPath);
        Map<String, List<String>> svcEntityMap = FileFactory.getSvcEntityMap(this.monitor, projectPath, callchain);

        List<String> msPaths = this.analyser.detectMS(namePathMap, svcEntityMap);
        System.out.println("STATIC ANALYSIS " +msPaths.toString());
        Map<String, Map<String, String>> svcDetails = new HashMap<>();
        double averageSize = svcEntityMap.entrySet().stream()
                // 过滤出 key 不在 msPaths 中的 entry
                .filter(entry -> !msPaths.contains(entry.getKey()))
                // 获取每个 entry 的 value 的 size
                .mapToInt(entry -> entry.getValue().size())
                // 计算平均值
                .average()
                // 如果存在值，则返回平均值，否则返回 0.0
                .orElse(0.0);
        for (String svc: svcEntityMap.keySet())
            System.out.println("svc"+ svc + "size: "+ svcEntityMap.get(svc).size());
        System.out.println("averageSize" +averageSize);
        for(String servicePath: msPaths) {
            System.out.println("callchain" + callchain.toString());
            Map<String, Map<String, String>> serviceModifiedDetails = this.planner.planGod(callchain.get(servicePath), servicePath,  (int) Math.floor(averageSize));
            if (serviceModifiedDetails != null)
                svcDetails.putAll(serviceModifiedDetails);
            for (Map.Entry<String, Map<String, String>> svcDetail : svcDetails.entrySet())
                executor.buildAndPushToHarbor(svcDetail);
        }
        return svcDetails;


    }

    public Map<String, Map<String, String>>  refactorNS(String projectPath) throws IOException, InterruptedException, ServiceException {
        Map<String, List<Node>>  callchain = this.monitor.getCommunities(projectPath);
        Map<String, String> namePathMap = FileFactory.getNamePathMap(this.monitor, projectPath);
        Map<String, List<String>> svcEntityMap = FileFactory.getSvcEntityMap(this.monitor, projectPath, callchain);
        int windowSize = 1;
        List<SvcTransRes> svcTransResList = this.monitor.getResList(windowSize).getData();
        Map<String,List<String>> resultPaths = this.analyser.detectNano(namePathMap, svcEntityMap);
        if(!resultPaths.get("nano").isEmpty()){
            Map<String, Map<String, String>> svcDetails = this.planner.planNano(projectPath, svcTransResList, resultPaths.get("normal"), resultPaths.get("nano"), svcEntityMap, namePathMap);
//
            for (Map.Entry<String, Map<String, String>> svcDetail: svcDetails.entrySet())
                executor.buildAndPushToHarbor(svcDetail);
            return svcDetails;
        }
        else return null;

    }

    public Map<String, Map<String, String>> refactorCS(String projectPath,  ServiceDetail serviceDetailTrransfer) throws IOException, XmlPullParserException, ServiceException {

        int windowSize = 1;
        List<SvcTransRes> svcTransResList = this.monitor.getResList(windowSize).getData();
        List<Set<String>>  resultCSCall = this.analyser.detectCS(svcTransResList);
        if (!resultCSCall.isEmpty()) {
            serviceDetailTrransfer.setResultCSCall(resultCSCall);
            Map<String, String> namePathMap = FileFactory.getNamePathMap(this.monitor, projectPath);
            Map<String, Map<String, String>> svcDetails = this.planner.planCS(resultCSCall, namePathMap);
            for (Map.Entry<String, Map<String, String>> svcDetail: svcDetails.entrySet())
                executor.buildAndPushToHarbor(svcDetail);
            return svcDetails;
        }
        else return null;

    }

    public Map<String, Map<String, String>> refactorSC(String projectPath,  ServiceDetail serviceDetailTrransfer) throws IOException, XmlPullParserException, ServiceException {

        int windowSize = 1;
        List<SvcTransRes> svcTransResList = this.monitor.getResList(windowSize).getData();
        Map<List<String>, Integer> resultMap = this.analyser.detectSC(svcTransResList);
        List<ChainKey> chainKeyList = new ArrayList<>();
        for (Map.Entry<List<String>, Integer> entry : resultMap.entrySet()) {
            ChainKey key = new ChainKey(entry.getKey(), entry.getValue());
            chainKeyList.add(key);
        }
        if (!chainKeyList.isEmpty()) {
//            serviceDetailTrransfer.setCsSetList(resultCSCall);
            serviceDetailTrransfer.setChainKeyList(chainKeyList);
            Map<String, String> namePathMap = FileFactory.getNamePathMap(this.monitor, projectPath);
            Map<String, Map<String, String>> svcDetails = this.planner.planSC(resultMap, namePathMap);
            for (Map.Entry<String, Map<String, String>> svcDetail: svcDetails.entrySet())
                executor.buildAndPushToHarbor(svcDetail);
            return svcDetails;
        }
        else return null;

    }
    public Map<String, Map<String, String>>  refactorNAV(String projectPath) throws IOException, ParseException, XmlPullParserException {
        Map<String, String> namePathMap = FileFactory.getNamePathMap(this.monitor, projectPath);
        List<String> servicePaths = FileFactory.getServicePaths(projectPath);
        Map<String, Map<String, UrlItem>> navDetails = this.analyser.detectNAV(servicePaths, namePathMap);
        System.out.println("navDetails" +navDetails.toString());
        if (!navDetails.isEmpty()) {
            Map<String, Map<String, String>> svcDetails = this.planner.planNAV(navDetails, namePathMap);
            if (svcDetails != null) {
                for (Map.Entry<String, Map<String, String>> svcDetail : svcDetails.entrySet())
                    executor.buildAndPushToHarbor(svcDetail);
                return svcDetails;
            } else
                return null;
        }
        else return null;
    }

    // REFACTOR Sharing Persistence
    public Map<String, Map<String, String>>  refactorSP(String projectPath) throws IOException {
        List<String> servicePaths = FileFactory.getServicePaths(projectPath);
        Map<String, List<Node>>  callchainMap = this.monitor.getCommunities(projectPath);
        Map<String, List<String>> svcEntityMap = FileFactory.getSvcEntityMap(this.monitor, projectPath, callchainMap);
        Map<String, String> namePathMap = FileFactory.getNamePathMap(this.monitor, projectPath);
        //每个set代表共享数据库的服务集合  value为service path
        List<Set<String>> spList = this.analyser.detectSP(projectPath, namePathMap);
        System.out.println("spList:" + spList.toString());
        if(spList != null && !spList.isEmpty()){
            Map<String, Map<String, String>> svcDetails = this.planner.planSP(spList, svcEntityMap);
            if (svcDetails != null) {
                for (Map.Entry<String, Map<String, String>> svcDetail : svcDetails.entrySet())
                    executor.buildAndPushToHarbor(svcDetail);
                return svcDetails;
            }
            else
                return null;
        }
        else return null;

    }

    public Map<String, Object> refactorNSDP(String projectPath) throws IOException, XmlPullParserException {
        Map<String, String> filePathToMicroserviceName = FileFactory.getFilePathToMicroserviceName(projectPath);
        Map<String, String> detectedResult = this.analyser.detectNSDP(projectPath, filePathToMicroserviceName);
        boolean hasDiscovery = true;
        for (String pattern: detectedResult.values()) {
            if (!pattern.contains("nacos") && !pattern.contains("eureka")) {
                hasDiscovery = false;
            }
        }
        if (!hasDiscovery) {
            System.out.println(detectedResult);
            return this.planner.planNSDP(projectPath, detectedResult);
        }
        return null;
    }

    public Map<String, Object> refactorNAG(String projectPath) throws Exception {
        Map<String, String> filePathToMicroserviceName = FileFactory.getFilePathToMicroserviceName(projectPath);
        int detectedResult = this.analyser.detectedNAG(projectPath, filePathToMicroserviceName);
        System.out.println(detectedResult);
        if (detectedResult != 2) {
            String discovery = "nacos";
            Map<String, String> nsdp = this.analyser.detectNSDP(projectPath, filePathToMicroserviceName);
            for (String value: nsdp.values()) {
                if (value.contains("eureka")) {
                    discovery = "eureka";
                } else if (value.contains("nacos")) {
                    discovery = "nacos";
                }
            }
            System.out.println(discovery);
            Long startTime = System.currentTimeMillis();
            Map<String, Object> result = this.planner.planNAG(projectPath, discovery);
            System.out.println("Time taken: " + (System.currentTimeMillis() - startTime) + " milliseconds");
            return result;
        }
        return null;
    }

    public Map<String, Object> refactorUS(String projectPath) throws XmlPullParserException, IOException {
        Map<String, String> filePathToMicroserviceName = FileFactory.getFilePathToMicroserviceName(projectPath);
        Map<String, String> detectedResult = this.analyser.detectedUS(projectPath, filePathToMicroserviceName);
        Map<String, String> nsdp = this.analyser.detectNSDP(projectPath, filePathToMicroserviceName);
        String discovery = "nacos";
        for (String value: nsdp.values()) {
            if (value.contains("eureka")) {
                discovery = "eureka";
            } else if (value.contains("nacos")) {
                discovery = "nacos";
            }
        }
        if (detectedResult.size() != filePathToMicroserviceName.size()) {
            return this.planner.planUS(projectPath, discovery);
            // System.out.println(detectedResult);
        }
        return null;
    }

    public Map<String, Object> refactorEBSI(String projectPath) throws IOException, XmlPullParserException {
        Map<String, List<String>> detectedResult = this.analyser.detectedEBSI(projectPath, FileFactory.getFilePathToMicroserviceName(projectPath));
        System.out.println(detectedResult);
        Long startTime = System.currentTimeMillis();
        Map<String, Object> result = this.planner.planEBSI(projectPath, detectedResult);
        System.out.println("Time taken: " + (System.currentTimeMillis() - startTime) + " milliseconds");
        return result;
    }

    public static void main(String[] args) throws IOException, ParseException, XmlPullParserException, InterruptedException {
        RAdaptiveSystem rAdaptiveSystem = new RAdaptiveSystem();
//        String projectPath = "D:\\code\\demo-collection\\Service-Demo";
        String projectPath = "D:\\code\\projects\\refactor-projects\\apollo-master\\apollo-master";

        Map<String, List<Node>>  callchainMap = rAdaptiveSystem.monitor.getCommunities(projectPath);
        Map<String, String> namePathMap = FileFactory.getNamePathMap(rAdaptiveSystem.monitor, projectPath);
        System.out.println("nameMap" + namePathMap.toString());
        Map<String, List<String>> svcEntityMap = FileFactory.getSvcEntityMap(rAdaptiveSystem.monitor, projectPath,callchainMap);
        System.out.println(rAdaptiveSystem.analyser.detectMS(namePathMap, null).toString());
//        rAdaptiveSystem.refactorNAV("D:\\code\\demo-collection\\Service-Demo2");
//        RefactorServiceImpl refactorService = new RefactorServiceImpl();
//        List<Node>  callchain = rAdaptiveSystem.monitor.getCommunities("D:\\code\\demo-collection\\Service-Demo\\testService");
//        LouvainAlgorithm algorithm = new LouvainAlgorithm(callchain);
//        List<Community> communities = algorithm.detectCommunities();
//        System.out.println("communities size" +communities.size()
//        );
//        for (Community community: communities)
//            System.out.println("communities" +community.toString());
//
//
//    }
    }
}
