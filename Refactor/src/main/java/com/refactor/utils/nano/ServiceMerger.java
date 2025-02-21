package com.refactor.utils.nano;
import com.refactor.dto.SvcCallDetail;
import com.refactor.trace.SvcTransRes;
import com.refactor.utils.FileFactory;
import com.refactor.utils.ParseResultUtils;
import com.refactor.utils.TraceUtils;
import com.refactor.utils.cohesion.CohesionAnalyzer;
import com.refactor.utils.cohesion.ServiceCohesion;
import lombok.Data;

import java.io.IOException;
import java.util.*;

/**
 * @description:
 * @author: xyc
 * @date: 2024-11-05 09:25
 */
@Data
public class ServiceMerger {
    public static double bestFitness;
    public static List<Integer> bestAllocation;
    public static HashMap<String, SvcCallDetail> serviceCallDetails;

    public static void main(String[] args) throws IOException {
        // 示例 nano services
//        nanoServices.add(new ServiceDetail("NanoService1", 5, 0.2, 0.8));
//        nanoServices.add(new ServiceDetail("NanoService2", 3, 0.3, 0.6));
        //获取调用关系
        String projectPath = "D:\\code\\demo-collection\\Service-Demo";
        ParseResultUtils parseResultUtils = new ParseResultUtils();
        serviceCallDetails = parseResultUtils.ESBUsageAnalysis(projectPath);
        List<SvcTransRes> svcTransResList = new ArrayList<>();
        List<String> normalServicePaths = new ArrayList<>();
        List<ServiceDetail> normalServices = new ArrayList<>();

        TraceUtils.svcTransResList = svcTransResList;
//        ServiceDetail service1 = new ServiceDetail("Service A", "",5, 1, 0.5);
//        ServiceDetail service2 = new ServiceDetail("NormalService2", "",70, 60, 30);
//        ServiceDetail service3 = new ServiceDetail("NormalService3", "",20, 60, 20);
//        normalServices.add(service1);
//        normalServices.add(service2);
//        normalServices.add(service3);

        Map<String, Double> cohesionMap = null;
        for(String normalServicePath: normalServicePaths){
            cohesionMap = CohesionAnalyzer.getServiceCohesion(normalServicePath);
            String svcName = FileFactory.getServiceDetails(normalServicePath).get("serviceName");
            SvcCallDetail svcCallDetail = serviceCallDetails.get(svcName);
            double coupling = svcCallDetail.getCalledService().size() + svcCallDetail.getCallService().size();
            //获取服务调用次数，接收类型为List<SvcTransRes>, 1.遍历获得总次数allCount，2.分析nano服务合并到正常服务的次数targetCount(每个调用链中正常服务调用nano服务的情况), 3. allCount - targetCount为合并后的总次数


            ServiceDetail service1 = new ServiceDetail(svcName, normalServicePath, TraceUtils.getServiceCallCounts(svcName), coupling, (double)cohesionMap.get("cohesion"), cohesionMap);
            normalServices.add(service1);
        }

        List<String> nanoServicePaths = new ArrayList<>();
        List<ServiceDetail> nanoServices = new ArrayList<>();
        for(String nanoServicePath: nanoServicePaths){
            cohesionMap = CohesionAnalyzer.getServiceCohesion(nanoServicePath);
            String svcName = FileFactory.getServiceDetails(nanoServicePath).get("serviceName");
            SvcCallDetail svcCallDetail = serviceCallDetails.get(svcName);
            double coupling = svcCallDetail.getCalledService().size() + svcCallDetail.getCallService().size();
            ServiceDetail service1 = new NanoService("Service A", nanoServicePath, TraceUtils.getServiceCallCounts(svcName), coupling, (double)cohesionMap.get("cohesion"), cohesionMap);
            nanoServices.add(service1);
        }


        // 示例 normal services
//        normalServices.add(new ServiceDetail("NormalService1", 10, 0.5, 0.7));
//        normalServices.add(new ServiceDetail("NormalService2", 7, 0.4, 0.8));
//        normalServices.add(new ServiceDetail("NormalService3", 8, 0.6, 0.65));


        // 存储当前分配
        List<Integer> allocation = new ArrayList<>();
        for (int i = 0; i < nanoServices.size(); i++) {
            allocation.add(0); // 初始化为0，表示未分配
        }

        // 开始递归分配
        assignNanoServices(nanoServices, normalServices, allocation, 0,0, 0);
        System.out.println("最佳分配: " + bestAllocation + " -> 最佳适应度: " + bestFitness);

        //文件合并，数据库内容合并，制作镜像并推送，修改路由

//        int systemCall = TraceUtils.getSystemCallCounts(svcTransResList) - TraceUtils.getReducedCounts(svcTransResList);
    }

    // 递归分配 nano services 到 normal services
    public static void assignNanoServices(List<ServiceDetail> nanoServices, List<ServiceDetail> normalServices,
                                           List<Integer> allocation, double totalServiceRank, double maxCoupling, int index) {
        if (index == nanoServices.size()) {
            // 如果所有 nano services 都分配完毕，计算适应度
            List<ServiceDetail> mergedNormalServices = mergeAllServices(nanoServices, normalServices, allocation);
            double mergedCohesion = 0;
            for (ServiceDetail normalService: mergedNormalServices){
                if (normalService.getName().contains("+")) {
                    ServiceCohesion serviceCohesion = new ServiceCohesion();
                    mergedCohesion = serviceCohesion.caculateCohesionByMap(normalService.getCohesionMap());
                    System.out.println("-------------");
                    System.out.println(normalService.toString());
                    System.out.println(normalService.getName() + ": " + serviceCohesion.toString());
                    break;

                }
            }
            System.out.println("mergedNormalServices" + mergedNormalServices.toString());
            double fitness = FitnessCalculator.calculateFitness(totalServiceRank, mergedNormalServices, mergedCohesion, maxCoupling);

            System.out.println("分配: " + allocation + " -> 适应度: " + fitness);
            if (fitness > bestFitness) {
                bestFitness = fitness;
                bestAllocation = new ArrayList<>(allocation);
            }
            return;
        }

        // 为当前 nano service 分配到每一个 normal service
        for (int i = 0; i < normalServices.size(); i++) {
            System.out.println(normalServices.get(i).getName());
            allocation.set(index, i); // 设置当前 nano service 分配到 normal service

            // 递归调用为下一个 nano service 分配
            assignNanoServices(nanoServices, normalServices, allocation, totalServiceRank, maxCoupling, index + 1);
        }
    }

    // 根据分配将所有服务合并
    private static List<ServiceDetail> mergeAllServices(List<ServiceDetail> nanoServices,
                                                        List<ServiceDetail> normalServices, List<Integer> allocation) {

        // 创建一个新的服务列表以保存合并后的服务
        List<ServiceDetail> mergedServices = new ArrayList<>(normalServices);

        // 遍历每个 nano service 进行合并
        for (int i = 0; i < nanoServices.size(); i++) {
            ServiceDetail nanoService = nanoServices.get(i);
            int normalServiceIndex = allocation.get(i); // 获取对应的 normal service 索引

            // 获取要合并的 normal service
            ServiceDetail normalService = mergedServices.get(normalServiceIndex);

            // 合并逻辑: 创建新的服务组合
            ServiceDetail mergedService = mergeServices(nanoService, normalService);

            // 更新合并后的服务到列表
            mergedServices.set(normalServiceIndex, mergedService);
        }

        return mergedServices;
    }

    // 合并一个 nano service 与一个 normal service
    private static ServiceDetail mergeServices(ServiceDetail nanoService, ServiceDetail normalService) {

        //对比 被调用Set 和 调用Set 取并集，计算合并后的coupling
        SvcCallDetail nanoDetail = serviceCallDetails.get(nanoService.getName());
        SvcCallDetail normalDetail = serviceCallDetails.get(normalService.getName());
        Set<String> mergedCallSet = new HashSet<>(nanoDetail.getCallService());
        mergedCallSet.addAll(normalDetail.getCallService());
        if (mergedCallSet.contains(nanoService.getName()))
            mergedCallSet.remove(nanoService.getName());

        Set<String> mergedCalledSet = new HashSet<>(nanoDetail.getCalledService());
        mergedCalledSet.addAll(normalDetail.getCalledService());
        if (mergedCalledSet.contains(normalService.getName()))
            mergedCalledSet.remove(normalService.getName());


        Map<String, Double>  nanoMap = nanoService.getCohesionMap();
        Map<String, Double>  normalMap = normalService.getCohesionMap();
        Map<String, Double> mergerdMap = new HashMap<>(nanoMap);
        for (String key: normalMap.keySet()){
            mergerdMap.put(key, normalMap.get(key) + nanoMap.get(key));
        }
        ServiceCohesion serviceCohesion = new ServiceCohesion();
        double totalCohesion = serviceCohesion.caculateCohesionByMap(mergerdMap);
        double totalCoupling = mergedCallSet.size() + mergedCalledSet.size();
        String normalName = normalService.getName();
        String nanoName = nanoService.getName();
        //计算合并后的调用次数
        int invocation = TraceUtils.getServiceCallCounts(normalName)  + TraceUtils.getServiceCallCounts(nanoName) - TraceUtils.getReducedCounts(normalName, nanoName);

        String newName = normalName + "+" + nanoName; // 创建新的服务名
        System.out.println("NAME" + newName + "totalCoupling： "+ totalCoupling + " totalCohesion：" +totalCohesion);

        return new ServiceDetail(newName, "", invocation, totalCoupling, totalCohesion, mergerdMap);
    }


    }