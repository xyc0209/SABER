//package com.refactor.utils.nano;
//import java.util.ArrayList;
//import java.util.List;
///**
// * @description:
// * @author: xyc
// * @date: 2024-11-05 09:25
// */
//public class ServiceMerger2 {
//    public static double bestFitness;
//    public static List<Integer> bestAllocation;
//
//    public static void main(String[] args) {
//        // 示例 nano services
//        List<ServiceDetail> nanoServices = new ArrayList<>();
////        nanoServices.add(new ServiceDetail("NanoService1", 5, 0.2, 0.8));
////        nanoServices.add(new ServiceDetail("NanoService2", 3, 0.3, 0.6));
//        NanoService nanoService1 = new NanoService("NanoService1", 5, 0.2, 0.8);
//        NanoService nanoService2 = new NanoService("NanoService2", 3, 0.3, 0.6);
//        nanoServices.add(nanoService1);
//        nanoServices.add(nanoService2);
//        // 示例 normal services
//        List<ServiceDetail> normalServices = new ArrayList<>();
////        normalServices.add(new ServiceDetail("NormalService1", 10, 0.5, 0.7));
////        normalServices.add(new ServiceDetail("NormalService2", 7, 0.4, 0.8));
////        normalServices.add(new ServiceDetail("NormalService3", 8, 0.6, 0.65));
//
//        ServiceDetail service1 = new ServiceDetail("Service A", 5, 1, 0.5);
//        ServiceDetail service2 = new ServiceDetail("NormalService2", 70, 60, 30);
//        ServiceDetail service3 = new ServiceDetail("NormalService3", 20, 60, 20);
//        normalServices.add(service1);
//        normalServices.add(service2);
//        normalServices.add(service3);
//        // 存储当前分配
//        List<Integer> allocation = new ArrayList<>();
//        for (int i = 0; i < nanoServices.size(); i++) {
//            allocation.add(0); // 初始化为0，表示未分配
//        }
//
//        // 开始递归分配
//        assignNanoServices(nanoServices, normalServices, allocation, 0);
//        System.out.println("最佳分配: " + bestAllocation + " -> 最佳适应度: " + bestFitness);
//    }
//
//    // 递归分配 nano services 到 normal services
//    private static void assignNanoServices(List<ServiceDetail> nanoServices, List<ServiceDetail> normalServices,
//                                           List<Integer> allocation, int index) {
//        if (index == nanoServices.size()) {
//            // 如果所有 nano services 都分配完毕，计算适应度
//            List<ServiceDetail> mergedNormalServices = mergeAllServices(nanoServices, normalServices, allocation);
//            double fitness = FitnessCalculator.calculateFitness(mergedNormalServices);
//
//            System.out.println("分配: " + allocation + " -> 适应度: " + fitness);
//            if (fitness > bestFitness) {
//                bestFitness = fitness;
//                bestAllocation = new ArrayList<>(allocation);
//            }
//            return;
//        }
//
//        // 为当前 nano service 分配到每一个 normal service
//        for (int i = 0; i < normalServices.size(); i++) {
//            allocation.set(index, i); // 设置当前 nano service 分配到 normal service
//
//            // 递归调用为下一个 nano service 分配
//            assignNanoServices(nanoServices, normalServices, allocation, index + 1);
//        }
//    }
//
//    // 根据分配将所有服务合并
//    private static List<ServiceDetail> mergeAllServices(List<ServiceDetail> nanoServices,
//                                                        List<ServiceDetail> normalServices, List<Integer> allocation) {
//
//        // 创建一个新的服务列表以保存合并后的服务
//        List<ServiceDetail> mergedServices = new ArrayList<>(normalServices);
//
//        // 遍历每个 nano service 进行合并
//        for (int i = 0; i < nanoServices.size(); i++) {
//            ServiceDetail nanoService = nanoServices.get(i);
//            int normalServiceIndex = allocation.get(i); // 获取对应的 normal service 索引
//
//            // 获取要合并的 normal service
//            ServiceDetail normalService = mergedServices.get(normalServiceIndex);
//
//            // 合并逻辑: 创建新的服务组合
//            ServiceDetail mergedService = mergeServices(nanoService, normalService);
//
//            // 更新合并后的服务到列表
//            mergedServices.set(normalServiceIndex, mergedService);
//        }
//
//        return mergedServices;
//    }
//
//    // 合并一个 nano service 与一个 normal service
//    private static ServiceDetail mergeServices(ServiceDetail nanoService, ServiceDetail normalService) {
//        int totalInvocations = normalService.invocations + nanoService.invocations;
//        double totalCoupling = normalService.coupling + nanoService.coupling;
//        double totalCohesion = normalService.cohesion + nanoService.cohesion; // 简单平均
//
//        String newName = normalService.name + "+" + nanoService.name; // 创建新的服务名
//        System.out.println("NAME" + newName + "totalCoupling： "+ totalCoupling + " totalCohesion：" +totalCohesion);
//        return new ServiceDetail(newName, totalInvocations, totalCoupling, totalCohesion);
//    }
//}