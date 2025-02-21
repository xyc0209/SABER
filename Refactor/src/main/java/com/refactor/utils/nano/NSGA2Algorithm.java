//package com.refactor.utils.nano;
//
///**
// * @description:
// * @author: xyc
// * @date: 2024-11-05 19:48
// */
//import java.util.*;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//
//public class NSGA2Algorithm {
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
//    public NSGA2Algorithm(List<ServiceDetail> serviceDetails, List<ServiceDetail> nanoServices) {
//        this.serviceDetails = serviceDetails;
//        this.numExistingServices = serviceDetails.size(); // 记录现有服务的数量
//        this.numNanoServices = nanoServices.size(); // 设置纳米服务数量
//        this.nanoServices = nanoServices;
//        population = new ArrayList<>();
//        initializePopulation();
//    }
//
//    // 初始化种群
//    private void initializePopulation() {
//        System.out.println(numExistingServices);
//        Random rand = new Random();  // 固定随机种子
//
//        for (int i = 0; i < populationSize; i++) {
//            List<Integer> individual = new ArrayList<>();
//            for (int j = 0; j < numNanoServices; j++) {
//                // 随机选择合并到的现有服务索引
//                int serviceIndex = rand.nextInt(numExistingServices);
//                individual.add(serviceIndex);
//            }
//            population.add(individual);
//        }
//        System.out.println(population.toString());
//    }
//
//    // 运行NSGA-II算法
//    public void run() {
//        for (int generation = 0; generation < numGenerations; generation++) {
//            System.out.println("generation " + generation);
//
//            // 用于存储合并后的服务和适应度列表
//            List<List<ServiceDetail>> mergedServicesList = new ArrayList<>();
//            List<Double[]> fitnessList = new ArrayList<>();
//
//            // 评估所有个体的适应度
//            for (List<Integer> individual : population) {
//                List<ServiceDetail> mergedServices = getMergedServices(individual);
//                Double[] fitness = FitnessCalculator.calculateFitnessNSGA(mergedServices);
//                for (int i = 0; i < fitness.length; i++) {
//                    System.out.println("fitness[" + i + "] = " + fitness[i]);
//                }
//                fitnessList.add(fitness);
//                mergedServicesList.add(mergedServices);
//            }
//
//            // 非支配排序
//            List<List<Integer>> fronts = nonDominatedSorting(fitnessList);
//
//            // 拥挤度计算
//            List<List<Integer>> crowdedFronts = calculateCrowdingDistance(fronts, fitnessList);
//
//            // 选择操作（根据非支配排序和拥挤度距离）
//            List<List<Integer>> selectedPopulation = selectByCrowdingDistance(crowdedFronts);
//
//            // 创建新种群，首先保留前50%的优秀个体
//            List<List<Integer>> newPopulation = new ArrayList<>(selectedPopulation.subList(0,selectedPopulation.size()/2));
//            int childIndex = 0;
//            // 交叉与变异操作
//            while (childIndex < population.size() / 6) {
//                // 选择两个父代个体
//                List<List<Integer>> parents = selectParents(selectedPopulation);
//                List<Integer> parent1 = parents.get(0);
//                List<Integer> parent2 = parents.get(1);
//
//                // 交叉操作
//                List<Integer> child = crossover(parent1, parent2);
//
//                // 变异操作
//                mutate(child);
//
//                // 添加到新种群
//                childIndex++;
//                newPopulation.add(child);
//            }
//
//            // 更新种群
//            population = newPopulation;
//
//            // 输出当前代的最佳适应度
//            double bestFitness = fitnessList.stream().mapToDouble(f -> f[1]).max().orElse(0);
//            System.out.println("Generation: " + generation + " - Best Fitness: " + bestFitness);
//            System.out.println(population.toString());
//            if (population.size()<=1)
//                break;
//        }
//    }
//
//    private List<List<Integer>> selectByCrowdingDistance(List<List<Integer>> crowdedFronts) {
//        List<List<Integer>> selectedPopulation = new ArrayList<>();
//
//        // 遍历每一个前沿，按拥挤度进行选择
//        for (List<Integer> front : crowdedFronts) {
//            for (Integer individualIndex : front) {
//                selectedPopulation.add(population.get(individualIndex));
//            }
//        }
//
//        return selectedPopulation;
//    }
//
//
//    // 非支配排序
//    private List<List<Integer>> nonDominatedSorting(List<Double[]> fitnessList) {
//        System.out.println("fitnessList" +fitnessList);
//        int populationSize = fitnessList.size();
//        List<List<Integer>> fronts = new ArrayList<>();
//        List<Integer> dominationCount = new ArrayList<>(Collections.nCopies(populationSize, 0));
//        List<List<Integer>> dominatedIndividuals = new ArrayList<>(populationSize);
//
//        for (int i = 0; i < populationSize; i++) {
//            dominatedIndividuals.add(new ArrayList<>());
//        }
//
//        // 遍历所有个体，计算支配关系
//        for (int i = 0; i < populationSize; i++) {
//            for (int j = i + 1; j < populationSize; j++) {
//                if (dominates(fitnessList.get(i), fitnessList.get(j))) {
//                    // i 支配 j
//                    System.out.println("fitnessList.get(i) " +fitnessList.get(i)[0]+"  "+fitnessList.get(i)[1]+"  "+fitnessList.get(i)[2]+ "fitnessList.get(j) " +fitnessList.get(j)[0]+"  "+fitnessList.get(j)[1]+"  "+fitnessList.get(j)[2]);
//                    dominatedIndividuals.get(i).add(j);
//                    dominationCount.set(j, dominationCount.get(j) + 1);
//                } else if (dominates(fitnessList.get(j), fitnessList.get(i))) {
//                    // j 支配 i
//                    dominatedIndividuals.get(j).add(i);
//                    dominationCount.set(i, dominationCount.get(i) + 1);
//                }
//            }
//        }
//
//        // 第一个前沿（等级0）是所有支配个体数为0的个体
//        List<Integer> firstFront = new ArrayList<>();
//        for (int i = 0; i < populationSize; i++) {
//            if (dominationCount.get(i) == 0) {
//                firstFront.add(i);
//                System.out.println("iiiiiiiii"+i);
//            }
//        }
//        fronts.add(firstFront);
//
//        // 依次计算剩余的前沿
//        int frontIndex = 0;
//        while (frontIndex < fronts.size()) {
//            System.out.println("+++++++++++++++");
//            List<Integer> nextFront = new ArrayList<>();
//            for (int i : fronts.get(frontIndex)) {
//                for (int j : dominatedIndividuals.get(i)) {
//                    dominationCount.set(j, dominationCount.get(j) - 1);
//                    if (dominationCount.get(j) == 0) {
//                        nextFront.add(j);
//                    }
//                }
//            }
//            if (!nextFront.isEmpty()) {
//                fronts.add(nextFront);
//            }
//            frontIndex++;
//        }
//
//        return fronts;
//    }
//
//
//    // 拥挤度计算
//    private List<List<Integer>> calculateCrowdingDistance(List<List<Integer>> fronts, List<Double[]> fitnessList) {
//        List<List<Integer>> crowdedFronts = new ArrayList<>();
//
//        for (List<Integer> front : fronts) {
//            int frontSize = front.size();
//            if (frontSize == 0) {
//                continue;
//            }
//
//            // 初始化所有个体的拥挤度为0
//            double[] crowdingDistance = new double[frontSize];
//            Arrays.fill(crowdingDistance, 0.0);
//
//            // 对每个目标进行排序并计算拥挤度
//            int numObjectives = fitnessList.get(0).length;
//            for (int obj = 0; obj < numObjectives; obj++) {
//                // 按当前目标值对前沿个体进行排序
//                List<Integer> sortedFront = new ArrayList<>(front);
//                final int index = obj;
//                sortedFront.sort(Comparator.comparingDouble(i -> fitnessList.get(i)[index]));
//
//                // 为排序后的个体计算拥挤度
//                crowdingDistance[0] = crowdingDistance[frontSize - 1] = Double.MAX_VALUE; // 边界个体的拥挤度设为最大值
//
//                for (int i = 1; i < frontSize - 1; i++) {
//                    double distance = fitnessList.get(sortedFront.get(i + 1))[obj] - fitnessList.get(sortedFront.get(i - 1))[obj];
//                    crowdingDistance[i] += distance;
//                }
//            }
//
//            // 按照拥挤度对前沿个体进行排序
//            List<Integer> sortedByCrowdingDistance = new ArrayList<>(front);
//            sortedByCrowdingDistance.sort((i1, i2) -> Double.compare(crowdingDistance[front.indexOf(i2)], crowdingDistance[front.indexOf(i1)]));
//
//            // 将排序后的个体加入到拥挤度排序后的前沿
//            crowdedFronts.add(sortedByCrowdingDistance);
//        }
//
//        return crowdedFronts;
//    }
//
//    private boolean dominates(Double[] fitness1, Double[] fitness2) {
//        boolean betterInAtLeastOneObjective = false;
//        boolean worseInAnyObjective = false;
//        for (int i = 0; i < fitness1.length; i++) {
//            if (i == 2) {  // cohesion 是越大越好
//                if (fitness1[i] > fitness2[i]) {
//                    betterInAtLeastOneObjective = true;
//                } else if (fitness1[i] < fitness2[i]) {
//                    worseInAnyObjective = true;
//                }
//            } else {  // invocations 和 coupling 是越小越好
//                if (fitness1[i] < fitness2[i]) {
//                    betterInAtLeastOneObjective = true;
//                } else if (fitness1[i] > fitness2[i]) {
//                    worseInAnyObjective = true;
//                }
//            }
//        }
//        return betterInAtLeastOneObjective && !worseInAnyObjective;
//    }
//
//    // 选择父代
//    private List<List<Integer>> selectParents(List<List<Integer>> population) {
//        Random rand = new Random();
//        return Arrays.asList(
//                population.get(rand.nextInt(population.size())),
//                population.get(rand.nextInt(population.size()))
//        );
//    }
//
//    // 交叉操作
//    private List<Integer> crossover(List<Integer> parent1, List<Integer> parent2) {
//        Random rand = new Random();
//        List<Integer> child = new ArrayList<>(parent1);
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
//                int newServiceIndex = rand.nextInt(numExistingServices);
//                individual.set(i, newServiceIndex);
//            }
//        }
//    }
//
//    // 根据每个NanoService的合并目标构建合并后的服务列表
//    private List<ServiceDetail> getMergedServices(List<Integer> individual) {
//        List<ServiceDetail> mergedServices = new ArrayList<>(serviceDetails.subList(0, numExistingServices));
//        for (int i = 0; i < individual.size(); i++) {
//            int targetServiceIndex = individual.get(i);
//            ServiceDetail targetService = mergedServices.get(targetServiceIndex);
//            // 合并逻辑可以在这里扩展
//            mergedServices.set(targetServiceIndex, new ServiceDetail(
//                    targetService.getName(),
//                    targetService.getInvocations() + nanoServices.get(i).getInvocations(),
//                    targetService.getCoupling() + nanoServices.get(i).getCoupling(),
//                    targetService.getCohesion() + nanoServices.get(i).getCohesion()
//            ));
//        }
//        return mergedServices;
//    }
//
//    // 主函数
//    public static void main(String[] args) {
//        // 创建几个正常服务
//        ServiceDetail service1 = new ServiceDetail("Service A", 5, 1, 0.5);
//        ServiceDetail service2 = new ServiceDetail("NormalService2", 70, 60, 30);
//        ServiceDetail service3 = new ServiceDetail("NormalService3", 20, 60, 20);
////        ServiceDetail service4 = new ServiceDetail("NormalService4", 9, 0.6, 0.65);
//
//        // 可用服务列表
//        List<ServiceDetail> availableServices = Arrays.asList(service1, service2, service3);
//
//        // 创建两个纳米服务
//        NanoService nanoService1 = new NanoService("NanoService1", 5, 0.2, 0.8);
//        NanoService nanoService2 = new NanoService("NanoService2", 3, 0.3, 0.6);
//
//        // 包含纳米服务的列表
//        List<ServiceDetail> nanoServices = Arrays.asList(nanoService1, nanoService2);
//
//        // 初始化NSGA-II算法，并运行
//        NSGA2Algorithm nsga2 = new NSGA2Algorithm(availableServices, nanoServices);
//        nsga2.run();
//    }
//}