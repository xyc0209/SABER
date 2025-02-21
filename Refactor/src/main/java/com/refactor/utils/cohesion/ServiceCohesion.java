package com.refactor.utils.cohesion;

import lombok.Data;

import java.util.Map;

/**
 * @description: Include metrics related to service cohesion
 * @author: xyc
 * @date: 2024-11-13 11:11
 */
@Data
public class ServiceCohesion {
    public double methodCallCohesion;
    public double dataCohesion;
    public double responsibilityCohesion;
    // Service Interface Data Cohesion
    public double SIDC;
    // Message-level Cohesion
    public double MC;

    public double caculateCohesion(){
        return (double) (this.methodCallCohesion + this.dataCohesion + this.responsibilityCohesion +this.SIDC + this.MC) / 5;
    }

    public double caculateCohesionByMap(Map<String, Double> cohesionMap){
        double controllerCount = cohesionMap.get("controllerCount");
        double serviceCount = cohesionMap.get("serviceCount");
        this.SIDC = cohesionMap.get("sidc") / controllerCount;
        this.MC = cohesionMap.get("locMessage") / controllerCount;
        this.methodCallCohesion = cohesionMap.get("methodCallRate") /  cohesionMap.get("serviceEntityCount");
        this.dataCohesion = cohesionMap.get("dataCohesion") / serviceCount;
        this.responsibilityCohesion = (1 / controllerCount + 1 / serviceCount) / 2;
        return this.caculateCohesion();
    }
}