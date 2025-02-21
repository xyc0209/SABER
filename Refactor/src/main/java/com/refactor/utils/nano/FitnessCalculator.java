package com.refactor.utils.nano;

import com.refactor.utils.cohesion.ServiceCohesion;
import com.refactor.utils.nano.ServiceDetail;

import java.util.List;

/**
 * @description: caculate fitness for merged services
 * @author: xyc
 * @date: 2024-11-04 19:54
 */
public class FitnessCalculator {

    public static double calculateFitness(double totalServiceRank, List<ServiceDetail> services, double mergedCohesion, double maxCoupling) {
        double w1 = 0.2, w2 = 0.2, w3 = 0.6; // 权重
        double lambda = 0.2; // 惩罚因子
        double k = 2;
        double mergedServiceRank = calculateTotalServiceRank(services);
        double mergedMaxCoupling = calculateMaxCoupling(services);
        double minCohesion = calculateMinCohesion(services);

        System.out.println("mergedServiceRank" +mergedServiceRank);
        System.out.println("mergedMaxCoupling" +mergedMaxCoupling);
        System.out.println("mergedCohesion" +mergedCohesion);
        System.out.println(1 - 1 / (2+ k * (totalServiceRank - mergedServiceRank)));
        System.out.println(1 - 1 / (2 + k * (maxCoupling- mergedMaxCoupling)));
        // 适应度函数
        return w1 * ( 1 - 1 / (2 + k * (totalServiceRank - mergedServiceRank))) +        // 0.1923
                w2 * ( 1 - 1 / (2 + k * (maxCoupling- mergedMaxCoupling))) +            // 0.15  0.48
                w3 * mergedCohesion; //
    }
    public static Double[] calculateFitnessNSGA(List<ServiceDetail> services) {
        double totalServiceRank = calculateTotalServiceRank(services);
        double maxCoupling = calculateMaxCoupling(services);
        double minCohesion = calculateMinCohesion(services);

        // 返回多目标适应度值数组
        return new Double[] {
                1 / (1 + totalServiceRank), // 目标1
                1 / (1 + maxCoupling),     // 目标2
                minCohesion                // 目标3
        };
    }
    public static double calculateTotalServiceRank(List<ServiceDetail> services) {
        double totalRank = 0;
        for (ServiceDetail service : services) {
            totalRank += service.invocations; // 调用次数作为总排名
        }
        return totalRank;
    }

    private static double calculateMaxCoupling(List<ServiceDetail> services) {
        double maxCoupling = 0;
        for (ServiceDetail service : services) {
            System.out.println(service.getName() +"service coupling" +service.coupling);
            maxCoupling = Math.max(maxCoupling, service.coupling); // 找到最大耦合度
        }
        System.out.println("---------------");
        return maxCoupling;
    }

    private static double calculateMinCohesion(List<ServiceDetail> services) {
        double minCohesion = Double.MAX_VALUE;
        for (ServiceDetail service : services) {
            minCohesion = Math.min(minCohesion, service.cohesion); // 找到最小内聚性
        }
        return minCohesion;
    }
}