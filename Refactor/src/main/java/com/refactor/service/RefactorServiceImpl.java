package com.refactor.service;

import com.github.javaparser.ParseException;
import com.refactor.Adaptive.RAdaptiveSystem;
import com.refactor.dto.RequestItem;
import com.refactor.dto.ServiceDetail;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @description:
 * @author: xyc
 * @date: 2024-10-16 17:57
 */
@Service
public class RefactorServiceImpl {
    @Autowired
    private RestTemplate restTemplate;
    @Value("${clusterAgent.ipandPort}")
    private  String clusterIPandPort;

    @Value("${detector.ipandPort}")
    private  String detectorIPandPort;

    @Value("${harbor.project}")
    private static  String harborProject;

    @Autowired
    private RAdaptiveSystem rAdaptiveSystem;
    private String getServiceUrl(String serviceName) {
        return "http://" + serviceName;
    }

    public String resloveMS(String projectPath, HttpHeaders httpHeaders) throws IOException, InterruptedException {
        System.out.println("clusterIPandPort"+clusterIPandPort);
        Map<String, Map<String, String>> serviceDetails = rAdaptiveSystem.refactorMS(projectPath);
        ServiceDetail serviceDetailTrransfer = new ServiceDetail();
//        serviceDetailTrransfer.setServiceDetail(serviceDetails);
//        System.out.println("serviceDetails: "+serviceDetails.toString());
//        HttpEntity requestEntity = new HttpEntity(serviceDetailTrransfer, httpHeaders);
//        ResponseEntity<String> re = restTemplate.exchange(
//                "http://" + clusterIPandPort + "/api/v1/clusteragent/deploy" ,
//                HttpMethod.POST,
//                requestEntity,
//                String.class);
//        return re.getBody();
        return null;
    }

    public String resloveCS(String projectPath, HttpHeaders httpHeaders) throws IOException, XmlPullParserException {
        System.out.println("clusterIPandPort"+clusterIPandPort);
        ServiceDetail serviceDetailTrransfer = new ServiceDetail();
        Map<String, Map<String, String>> serviceDetails = rAdaptiveSystem.refactorCS(projectPath, serviceDetailTrransfer);
        serviceDetailTrransfer.setServiceDetail(serviceDetails);
        System.out.println("serviceDetails: "+serviceDetails.toString());
        HttpEntity requestEntity = new HttpEntity(serviceDetailTrransfer, httpHeaders);
        ResponseEntity<String> re = restTemplate.exchange(
                "http://" + clusterIPandPort + "/api/v1/clusteragent/deployCS" ,
                HttpMethod.POST,
                requestEntity,
                String.class);
        return re.getBody();
    }

    public String resloveSC(String projectPath, HttpHeaders httpHeaders) throws IOException, XmlPullParserException {
        System.out.println("clusterIPandPort"+clusterIPandPort);
        ServiceDetail serviceDetailTrransfer = new ServiceDetail();
        Map<String, Map<String, String>> serviceDetails = rAdaptiveSystem.refactorSC(projectPath, serviceDetailTrransfer);
        serviceDetailTrransfer.setServiceDetail(serviceDetails);
        System.out.println("serviceDetails: "+serviceDetails.toString());
        HttpEntity requestEntity = new HttpEntity(serviceDetailTrransfer, httpHeaders);
        ResponseEntity<String> re = restTemplate.exchange(
                "http://" + clusterIPandPort + "/api/v1/clusteragent/deploySC" ,
                HttpMethod.POST,
                requestEntity,
                String.class);
        return re.getBody();
    }
    public String resloveTest(RequestItem requestItem, HttpHeaders httpHeaders) throws IOException, XmlPullParserException, ParseException {
        System.out.println("clusterIPandPort"+clusterIPandPort);

        HttpEntity requestEntity = new HttpEntity(requestItem, httpHeaders);
        ResponseEntity<String> re = restTemplate.exchange(
                "http://" + clusterIPandPort + "/api/v1/clusteragent/test" ,
                HttpMethod.POST,
                requestEntity,
                String.class);
        return re.getBody();
    }
    public String resloveNAV(String projectPath, HttpHeaders httpHeaders) throws IOException, XmlPullParserException, ParseException {
        System.out.println("clusterIPandPort"+clusterIPandPort);
        ServiceDetail serviceDetailTrransfer = new ServiceDetail();
        Map<String, Map<String, String>> serviceDetails = rAdaptiveSystem.refactorNAV(projectPath);
//        serviceDetailTrransfer.setServiceDetail(serviceDetails);
//        System.out.println("serviceDetails: "+serviceDetails.toString());
//        HttpEntity requestEntity = new HttpEntity(serviceDetailTrransfer, httpHeaders);
//        ResponseEntity<String> re = restTemplate.exchange(
//                "http://" + clusterIPandPort + "/api/v1/clusteragent/deploy" ,
//                HttpMethod.POST,
//                requestEntity,
//                String.class);
//        return re.getBody();
        return "SUCCESS";
    }
    public String resloveNS(String projectPath, HttpHeaders httpHeaders) throws IOException, InterruptedException {
        Map<String, Map<String, String>> serviceDetails = rAdaptiveSystem.refactorNS(projectPath);
//        ServiceDetail serviceDetailTrransfer = new ServiceDetail();
//        serviceDetailTrransfer.setServiceDetail(serviceDetails);
//        System.out.println("serviceDetails: "+serviceDetails.toString());
//        HttpEntity requestEntity = new HttpEntity(serviceDetailTrransfer, httpHeaders);
//        ResponseEntity<String> re = restTemplate.exchange(
//                "http://" + clusterIPandPort + "/api/v1/clusteragent/deploy" ,
//                HttpMethod.POST,
//                requestEntity,
//                String.class);
//        return re.getBody();
        return null;

    }
    public String resloveSP(String projectPath, HttpHeaders httpHeaders) throws IOException {

        Map<String, Map<String, String>> spDetails = rAdaptiveSystem.refactorSP(projectPath);  //projectPath
        if (spDetails == null)
            return "No code refactoring to eliminate SP or ISI";
        ServiceDetail serviceDetailTrransfer = new ServiceDetail();
        serviceDetailTrransfer.setServiceDetail(spDetails);
        System.out.println("serviceDetails: "+spDetails.toString());
        HttpEntity requestEntity1 = new HttpEntity(serviceDetailTrransfer, httpHeaders);
        ResponseEntity<String> re = restTemplate.exchange(
                "http://" + clusterIPandPort + "/api/v1/clusteragent/deploy" ,
                HttpMethod.POST,
                requestEntity1,
                String.class);
        return re.getBody();
    }

    public String resloveTEST(String projectPath, HttpHeaders httpHeaders) throws IOException {
        RestTemplate restTemplate1 = new RestTemplate();
        HttpEntity requestEntity1 = new HttpEntity(null, httpHeaders);
        ResponseEntity<String> re = restTemplate1.exchange(
                "http://" + clusterIPandPort + "/api/v1/clusteragent/test" ,
                HttpMethod.GET,
                requestEntity1,
                String.class);
        return re.getBody();
    }
    public String resolveNAG(String projectPath, HttpHeaders httpHeaders) throws Exception {
        Map<String, Object> modificationInfo = rAdaptiveSystem.refactorNAG(projectPath);
        if (modificationInfo == null)
            return "No code refactoring to eliminate NAG";
        HttpEntity requestEntity = new HttpEntity(modificationInfo.getOrDefault("serviceModifiedDetails", null), httpHeaders);
        ResponseEntity<String> re = restTemplate.exchange(
                "http://" + clusterIPandPort + "api/v1/clusteragent/deployNAG",
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        return re.getBody();
    }

    /**
     * No Service Discovery Pattern
     */
    public String resolveNSDP(String projectPath, HttpHeaders httpHeaders) throws IOException, XmlPullParserException {
        Map<String, Object> modificationInfo = rAdaptiveSystem.refactorNSDP(projectPath);
        HttpEntity requestEntity = new HttpEntity(modificationInfo.getOrDefault("serviceModifiedDetails", null), httpHeaders);
        ResponseEntity<String> re = restTemplate.exchange(
                "http://" + clusterIPandPort + "api/v1/clusteragent/deployNSDP",
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        return re.getBody();
    }

    /**
     * Unnecessary Settings
     */
    public String resolveUS(String projectPath, HttpHeaders httpHeaders) throws XmlPullParserException, IOException {
        Map<String, Object> modificationInfo = rAdaptiveSystem.refactorUS(projectPath);
        if (modificationInfo == null)
            return "No code refactoring to eliminate US";
        HttpEntity requestEntity = new HttpEntity(modificationInfo.getOrDefault("serviceModifiedDetails", null), httpHeaders);
        ResponseEntity<String> re = restTemplate.exchange(
                "http://" + clusterIPandPort + "api/v1/clusteragent/deployUS",
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        return re.getBody();
    }

    /**
     * Endpoint Based Service Interaction
     */
    public String resolveEBSI(String projectPath, HttpHeaders httpHeaders) throws IOException {
        Map<String, Object> modificationInfo = rAdaptiveSystem.refactorEBSI(projectPath);
        HttpEntity requestEntity = new HttpEntity(modificationInfo.getOrDefault("serviceModifiedDetails", null), httpHeaders);
        ResponseEntity<String> re = restTemplate.exchange(
                "http://" + clusterIPandPort + "api/v1/clusteragent/deployEBSI",
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        return re.getBody();
    }
}