package com.refactor.utils.nano;

import java.util.Map;

/**
 * @description:
 * @author: xyc
 * @date: 2024-11-04 21:21
 */
public  class NanoService extends ServiceDetail {
    public NanoService(String name, String servicePath, int invocations, double coupling, double cohesion, Map<String, Double> cohesionMap) {
        super(name, servicePath, invocations, coupling, cohesion, cohesionMap);
    }


}