package com.refactor.executor.controller;

import com.refactor.executor.agent.IngressAgent;
import com.refactor.executor.agent.K8sClusterAgent;
import com.refactor.executor.dto.ChainKey;
import com.refactor.executor.dto.RequestItem;
import com.refactor.executor.dto.ServiceDetail;
import com.refactor.executor.dto.ServiceTest;
import com.refactor.executor.pod.PodInfo;
import io.kubernetes.client.openapi.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @description:
 * @author: xyc
 * @date: 2024-09-29 14:15
 */
@RequestMapping("/api/v1/clusteragent")
@RestController
public class AgentController {

    @Autowired
    K8sClusterAgent k8sClusterAgent;
    @Autowired
    IngressAgent ingressAgent;

    @GetMapping("/yaml")
    public String makeYaml(){
        System.out.println(
                k8sClusterAgent.deploySVC(
                        k8sClusterAgent.createDeploymentFromImage("10.245.1.233:5000/testreposity/testserviceimage:split_1", "service-split", 8089)));
        return "";
    }

    @GetMapping("/make")
    public String make(){
        System.out.println( k8sClusterAgent.createDeploymentFromImage("10.245.1.233:5000/testreposity/testserviceimage:split_1", "service-split", 8089));
        return "Make success";
    }

    @PostMapping("/deploy")
    public String deployBatch(@RequestBody ServiceDetail serviceDetail){
        System.out.println("serviceDetail"+serviceDetail.toString());
        Map<String, Map<String, String>> svcDetail =serviceDetail.getServiceDetail();
        //deploy new resources and delete old rresources
        System.out.println(k8sClusterAgent.createDeploymentByBatch(svcDetail));

        //delete original resources
//        k8sClusterAgent.deleteResourcesByBatch(svcDetail);

        // update routes, if ingress exists
        ingressAgent.operateIngressByBatch(k8sClusterAgent.getClient(), svcDetail);
        return "ALL OPERATIONS SUCCESS";
    }

    @PostMapping("/deployNAV")
    public String deployNAVBatch(@RequestBody ServiceDetail serviceDetail){
        System.out.println("serviceDetail"+serviceDetail.toString());
        Map<String, Map<String, String>> svcDetail =serviceDetail.getServiceDetail();
        //deploy new resources
        System.out.println(k8sClusterAgent.createDeploymentByBatch(svcDetail));

        //delete original resources
        k8sClusterAgent.deleteResourcesByBatch(svcDetail);

        // update routes, if ingress exists
        ingressAgent.operateIngressByBatch(k8sClusterAgent.getClient(),svcDetail);
        return "ALL OPERATIONS SUCCESS";
    }

    @GetMapping("/podinfo")
    public Map<String, List<PodInfo>> getPodInfo() throws ApiException {
        return k8sClusterAgent.getPodsADetails();
    }

    @PostMapping("/deployCS")
    public String deployCSBatch(@RequestBody ServiceDetail serviceDetail) throws ApiException {
        Map<String, Map<String, String>> svcDetail =serviceDetail.getServiceDetail();
        List<Set<String>> resultCSCall = serviceDetail.getResultCSCall();
        System.out.println("serviceDetail"+resultCSCall);

        k8sClusterAgent.createDeploymentCSByBatch(resultCSCall, svcDetail);

        return "ALL OPERATIONS SUCCESS";
    }
    @PostMapping("/deploySC")
    public String deploySCBatch(@RequestBody ServiceDetail serviceDetail) throws ApiException {
        Map<String, Map<String, String>> svcDetail =serviceDetail.getServiceDetail();
        List<ChainKey> chainKeyList = serviceDetail.getChainKeyList();
        System.out.println("chainMap"+chainKeyList);

        k8sClusterAgent.createDeploymentSCByBatch(chainKeyList, svcDetail);

        return "ALL OPERATIONS SUCCESS";
    }
//    @GetMapping("/Ingress")
//    public String updateIngress(){
//        return IngressAgent.updateIngress("/api1/test", "service-split", 8089);
//    }

    @PostMapping("/resources")
    public String deleteResources(@RequestBody RequestItem requestItem){
        k8sClusterAgent.deleteResources(requestItem.getServiceList(), requestItem.getNamespace());
        return "Resources delete success";
    }

    @PostMapping("/deployNSDP")
    public String deployNSDPBatch(@RequestBody ServiceDetail serviceDetail) throws ApiException {
        Map<String, Map<String, String>> svcDetail = serviceDetail.getServiceDetail();
        k8sClusterAgent.createDeploymentByBatch(svcDetail);
        k8sClusterAgent.deleteResourcesByBatch(svcDetail);
        ingressAgent.operateIngressByBatch(k8sClusterAgent.getClient(), svcDetail);
        return "ALL OPERATIONS SUCCESS";
    }

    @PostMapping("/deployUS")
    public String deployUSBatch(@RequestBody ServiceDetail serviceDetail) throws ApiException {
        Map<String, Map<String, String>> svcDetail = serviceDetail.getServiceDetail();
        k8sClusterAgent.createDeploymentByBatch(svcDetail);
        k8sClusterAgent.deleteResourcesByBatch(svcDetail);
        ingressAgent.operateIngressByBatch(k8sClusterAgent.getClient(), svcDetail);
        return "ALL OPERATIONS SUCCESS";
    }

    @PostMapping("/deployNAG")
    public String deployNAGBatch(@RequestBody ServiceDetail serviceDetail) throws ApiException {
        System.out.println(serviceDetail);
        Map<String, Map<String, String>> svcDetail = serviceDetail.getServiceDetail();
        k8sClusterAgent.deleteResourcesByBatch(svcDetail);
        k8sClusterAgent.createDeploymentByBatch(svcDetail);
        // k8sClusterAgent.deleteResourcesByBatch(svcDetail);
        ingressAgent.operateIngressByBatch(k8sClusterAgent.getClient(), svcDetail);
        return "ALL OPERATIONS SUCCESS";
    }

    @PostMapping("/deployEBSI")
    public String deployEBSIBatch(@RequestBody ServiceDetail serviceDetail) throws ApiException {
        Map<String, Map<String, String>> svcDetail = serviceDetail.getServiceDetail();
        k8sClusterAgent.deleteResourcesByBatch(svcDetail);
        k8sClusterAgent.createDeploymentByBatch(svcDetail);
        // k8sClusterAgent.deleteResourcesByBatch(svcDetail);
        ingressAgent.operateIngressByBatch(k8sClusterAgent.getClient(), svcDetail);
        return "ALL OPERATIONS SUCCESS";
    }


    @PostMapping("/test")
    public String testAPI(@RequestBody ServiceDetail serviceDetail) throws IOException, ApiException {
        System.out.println("serviceDetail"+serviceDetail.toString());
        System.out.println("-----");
        return "test sucess";
    }

    @PostMapping("/test2")
    public String testAPI2(@RequestBody ServiceDetail serviceDetail) throws IOException, ApiException {
        System.out.println("serviceDetail"+serviceDetail.toString());
        System.out.println("-----");
        return "test sucess";
    }

}