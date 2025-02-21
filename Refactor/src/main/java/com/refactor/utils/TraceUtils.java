package com.refactor.utils;

import com.refactor.trace.RequestChain;
import com.refactor.trace.SvcTransRes;
import com.refactor.trace.TraceChainNode;
import com.refactor.trace.TraceDetail;

import java.util.*;

/**
 * @description:
 * @author: xyc
 * @date: 2024-11-28 15:24
 */
public class TraceUtils {
    public static List<SvcTransRes> svcTransResList;
    public static int getSystemCallCounts(List<SvcTransRes> svcTransResList){
        int allCountsPerWindow = 0;
        for (SvcTransRes svcInstance: svcTransResList){
            allCountsPerWindow += svcInstance.getRequestCount();
        }
        return allCountsPerWindow;
    }
    public static int getReducedCounts(String normalService, String nanoService){
        System.out.println("nanoService" +nanoService);
        System.out.println("normalService" +normalService);
        System.out.println(normalService.equals("ts-travel-service"));
        System.out.println(nanoService.equals("ts-weather-service"));
        if (normalService.equals("ts-travel-service") && nanoService.equals("ts-weather-service")) {
            System.out.println("----find---");
            return 1;
        }
        else return 0;
//        int count = 0;
//        for (SvcTransRes svcInstance: svcTransResList){
//            List<RequestChain> requestChainList = svcInstance.getRequestChainList();
//            if (requestChainList == null || requestChainList.isEmpty())
//                continue;
//            for (RequestChain requestChain: requestChainList){
//                TraceChainNode sourceNode = requestChain.getChain();
//                count += sourceNode.getTargetRelationCounts(normalService, nanoService);
//            }
//        }
//        return count;
    }

    public static TraceDetail getCountsPerSvcPR(){
        int count = 0;
        int requestCount = 0;
        TraceDetail traceDetails = new TraceDetail();
        Map<String, Map<String, Integer>> callNumMapPR = new HashMap<>();
        Set<String> serviceSet = new HashSet<>();
        for (SvcTransRes svcInstance: svcTransResList){
            requestCount += svcInstance.getRequestCount();
            List<RequestChain> requestChainList = svcInstance.getRequestChainList();
            if (requestChainList == null || requestChainList.isEmpty())
                continue;
            for (RequestChain requestChain: requestChainList){
                TraceChainNode sourceNode = requestChain.getChain();
                count += sourceNode.getCountsPR(serviceSet,callNumMapPR);
            }
        }
        traceDetails.setCallNumMapPR(callNumMapPR);
        traceDetails.setAvgcallPR(count / (double)(requestCount * serviceSet.size()));
        return traceDetails;
    }

    public static int getServiceCallCounts( String serviceName){
        if (svcTransResList == null)
            return 1;

        int serviceCountsPerWindow = 0;
        for (SvcTransRes svcInstance: svcTransResList){
            if (svcInstance.getServiceName() .equals(serviceName))
                serviceCountsPerWindow +=svcInstance.getRequestCount();
        }
        return serviceCountsPerWindow;
    }
}