//package com.refactor.utils.nano;
//
//import java.util.*;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//
///**
// * @description: Genetic algorithm to implement service merging to eliminate nano service
// * @author: xyc
// * @date: 2024-11-04 19:58
// */
//public class GeneticAlgorithm {
//    private List<List<Integer>> population; // 每个个体是一个索引列表，表示每个NanoService的合并目标
//    private final int populationSize = 20; // 种群大小
//    private final int numGenerations = 10; // 代数
//    private final double mutationRate = 0.1; // 变异率
//    private final int numNanoServices; // nano service 数量
//    private final int numExistingServices; // 现有服务数量
//    private List<ServiceDetail> serviceDetails; // 所有可用服务列表
//
//    private List<ServiceDetail> nanoServices;
//
//    public GeneticAlgorithm(List<ServiceDetail> serviceDetails, List<ServiceDetail> nanoServices) {
//        this.serviceDetails = serviceDetails;
//        this.numExistingServices = serviceDetails.size(); // 记录现有服务的数量
//        this.numNanoServices = nanoServices.size(); // 设置纳米服务数量
//        this.nanoServices = nanoServices;
//        population = new ArrayList<>();
//        initializePopulation();
//    }
//
//    // 初始化种群
////    private void initializePopulation() {
////        Random rand = new Random();
////        for (int i = 0; i < populationSize; i++) {
////            List<Integer> individual = new ArrayList<>();
////            for (int j = 0; j < numNanoServices; j++) {
////                // 随机选择合并到的现有服务索引
////                int serviceIndex = rand.nextInt(numExistingServices - numNanoServices);
////                individual.add(serviceIndex);
////            }
////            population.add(individual);
////        }
////        System.out.println("population: "+ population.toString());
////    }
//    private void initializePopulation() {
//        population = new ArrayList<>();
//        generateAllCombinations(new ArrayList<>(), 0); // 开始生成所有组合
//
//        // 随机填充到目标种群大小
//        Random rand = new Random();
//        while (population.size() < populationSize) {
//            List<Integer> individual = new ArrayList<>();
//            for (int j = 0; j < numNanoServices; j++) {
//                int serviceIndex = rand.nextInt(numExistingServices - numNanoServices);
//                individual.add(serviceIndex);
//            }
//            population.add(individual);
//        }
//
//        System.out.println("Population: " + population.toString());
//    }
//
//    // 递归生成所有可能的组合
//    private void generateAllCombinations(List<Integer> current, int depth) {
//        // 基本情况：如果达到纳米服务数量
//        if (depth == numNanoServices) {
//            population.add(new ArrayList<>(current)); // 添加当前组合到种群
//            return;
//        }
//
//        // 递归生成每个深度的组合
//        for (int i = 0; i < numExistingServices - numNanoServices; i++) {
//            current.add(i); // 添加当前服务索引
//            generateAllCombinations(current, depth + 1); // 进入下一个深度
//            current.remove(current.size() - 1); // 回溯，移除最近添加的服务索引
//        }
//    }
//
//    // 运行遗传算法
//    public void run() {
//        for (int generation = 0; generation < numGenerations; generation++) {
//            System.out.println("generation" +generation);
//            List<Double> fitnessList = new ArrayList<>();
//
////             评估所有个体的适应度
//            for (List<Integer> individual : population) {
//                // 计算个体的适应度
//                double fitness = FitnessCalculator.calculateFitness(getMergedServices(individual));
//                fitnessList.add(fitness);
//            }
//            if (population.size() == 1) {
//                System.out.println("target: " + population.get(0) + "best fitness:" + fitnessList.get(0));
//                break;
//            }
//            // 找出适应度较高的个体
//            List<List<Integer>> retainedIndividuals = retainBestIndividuals(fitnessList);
//            // 创建新种群并进行交叉与变异
//            List<List<Integer>> newPopulation = new ArrayList<>(retainedIndividuals);
//            // 随机选择适应度较高的个体进行交叉和变异
//            List<List<Integer>> children = new ArrayList<>();
//            while (children.size() < newPopulation.size() / 6) {
//                List<List<Integer>> parents = selectRandom(2, retainedIndividuals);
//                List<Integer> parent1 = parents.get(0);
//                List<Integer> parent2 = parents.get(1);
//
//                // 交叉生成子代
//                List<Integer> child = crossover(parent1, parent2);
//                // 变异
//                mutate(child);
//                children.add(child);
//            }
//            // 合并保留的个体与新生成的个体
//            newPopulation.addAll(children);
//            // 更新种群为新种群
//            population = newPopulation;
//            // 输出当前代的最佳适应度
//
//            double bestFitness = (fitnessList.size() >= 1) ? Collections.max(fitnessList) : fitnessList.get(0);
//            System.out.println("population size"+ population.size());
//            System.out.println("Generation: " + generation + " - Best Fitness: " + bestFitness);
////
////            if (population.size() == 1)
////                break;
//        }
//    }
//
//    // 保留适应度较高的个体
//    private List<List<Integer>> retainBestIndividuals(List<Double> fitnessList) {
//        List<Integer> indices = IntStream.range(0, population.size())
//                .boxed()
//                .sorted((i1, i2) -> Double.compare(fitnessList.get(i2), fitnessList.get(i1))) // 根据适应度降序排序
//                .collect(Collectors.toList());
//
//        List<List<Integer>> retained = new ArrayList<>();
//        for (int i = 0; i < population.size() / 2; i++) {
//            retained.add(population.get(indices.get(i)));
//        }
//        return retained;
//    }
//
//    // 随机选择个体
//    private List<List<Integer>> selectRandom(int n, List<List<Integer>> individuals) {
//        Set<List<Integer>> selectedIndividuals = new HashSet<>();
//        Random rand = new Random();
//
//        // 随机选择 n 个不同的个体
//        while (selectedIndividuals.size() < n) {
//            int index = rand.nextInt(individuals.size());
//            selectedIndividuals.add(individuals.get(index));
//        }
//
//        return new ArrayList<>(selectedIndividuals); // 将集合转换为列表并返回
//    }
//
//    // 交叉操作
//    private List<Integer> crossover(List<Integer> parent1, List<Integer> parent2) {
//        Random rand = new Random();
//        List<Integer> child = new ArrayList<>(parent1); // 复制parent1的基因
//        int crossoverPoint = rand.nextInt(numNanoServices); // 随机选择一个交叉点
//
//        // 从parent2中拷贝基因到child
//        for (int i = crossoverPoint; i < numNanoServices; i++) {
//            child.set(i, parent2.get(i));
//        }
//
//        return child;
//    }
//
//    // 变异操作
//    private void mutate(List<Integer> individual) {
//        Random rand = new Random();
//
//        for (int i = 0; i < numNanoServices; i++) {
//            // 根据变异率决定是否进行变异
//            if (rand.nextDouble() < mutationRate) {
//                // 随机替换为一个新的服务的索引
//                int newServiceIndex = rand.nextInt(numExistingServices - numNanoServices);
//                individual.set(i, newServiceIndex);
//            }
//        }
//    }
//
//    // 根据每个NanoService的合并目标构建合并后的服务列表
//    private List<ServiceDetail> getMergedServices(List<Integer> individual) {
//        System.out.println("individual" +individual);
//        List<ServiceDetail> mergedServices = new ArrayList<>(serviceDetails.subList(0,numExistingServices - numNanoServices));
//        for (int i = 0; i < individual.size(); i++) {
//            int targetServiceIndex = individual.get(i);
//            ServiceDetail targetService = mergedServices.get(targetServiceIndex);
//            System.out.println(mergedServices.toString());
//            // 合并逻辑可以在这里扩展
//            // 假设合并后生成一个新的服务
//            mergedServices.set(targetServiceIndex, new ServiceDetail(
//                    targetService.getName(),
//                    targetService.getInvocations() + nanoServices.get(i).getInvocations(), // 合并的调用次数
//                    targetService.getCoupling() + nanoServices.get(i).getCoupling(), // 取最大故障率
//                    targetService.getCohesion() + nanoServices.get(i).getCohesion()
//            ));
//        }
//        System.out.println(mergedServices.toString());
//        return mergedServices; // 返回合并后的服务列表
//    }
//
//    // 随机生成纳米服务的调用次数、故障率和响应时间（可以根据实际需要进行更改）
//    private int randomNanoServiceInvocation() {
//        return new Random().nextInt(100); // 模拟纳米服务调用次数（可以根据实际情况调整）
//    }
//
//    private double randomNanoServiceCoupling() {
//        return new Random().nextDouble(); // 模拟纳米服务的故障率（可以根据实际情况调整）
//    }
//
//    private double randomNanoServiceCohesion() {
//        return new Random().nextDouble(); // 模拟纳米服务的响应时间（可以根据实际情况调整）
//    }
//
//    // 主函数
//    // 主函数
//    public static void main(String[] args) {
//        // 创建几个正常服务
////        ServiceDetail service1 = new ServiceDetail("Service A", 10, 0.5, 0.7);
////        ServiceDetail service2 = new ServiceDetail("NormalService2", 7, 0.4, 0.8);
////        ServiceDetail service3 = new ServiceDetail("NormalService3", 8, 0.6, 0.65);
////        ServiceDetail service4 = new ServiceDetail("NormalService4", 9, 0.6, 0.65);
//        ServiceDetail service1 = new ServiceDetail("Service A", 5, 1, 0.5);
//        ServiceDetail service2 = new ServiceDetail("NormalService2", 70, 60, 30);
//        ServiceDetail service3 = new ServiceDetail("NormalService3", 20, 60, 20);
//        // 可用服务列表
//        List<ServiceDetail> availableServices = Arrays.asList(service1, service2, service3);
//
//        // 创建两个纳米服务
//        NanoService nanoService1 = new NanoService("NanoService1", 5, 0.2, 0.8);
//        NanoService nanoService2 = new NanoService("NanoService2", 3, 0.3, 0.6);
//
//
//
//        // 包含纳米服务的列表
//        List<ServiceDetail> nanoServices = Arrays.asList(nanoService1, nanoService2);
//
//        // 将所有可用服务合并到一个列表中
//        List<ServiceDetail> allServices = new ArrayList<>();
//        allServices.addAll(availableServices);
//        allServices.addAll(nanoServices);
//
////        for(int i=0; i<availableServices.size(); i++){
////            for(int j=0; j<availableServices.size(); j++){
////
////            }
////        }
//
////         初始化遗传算法，并运行
//        GeneticAlgorithm ga = new GeneticAlgorithm(allServices, nanoServices); // 传入所有服务和纳米服务列表
//        ga.run(); // 运行遗传算法
//
//    }
//}