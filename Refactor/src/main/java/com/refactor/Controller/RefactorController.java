package com.refactor.Controller;

import com.github.javaparser.ParseException;
import com.google.protobuf.ServiceException;
import com.refactor.dto.RequestItem;
import com.refactor.service.RefactorServiceImpl;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import com.refactor.agent.K8sClusterAgent;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/mape")
public class RefactorController {
    @Autowired
    private RefactorServiceImpl refactorService;

    @GetMapping("/test")
    public String report(){
        return "";
    }

    @GetMapping("/yaml")
    public String makeYaml(){
        System.out.println(K8sClusterAgent.createDeploymentFromImage("172.16.17.46:8085/testreposity/testserviceimage:split_1", "ts-verification-code-service", 8089));
        return "";
    }
    @PostMapping("/refactor/ms")
    public String resloveMS(@RequestBody RequestItem requestItem, @RequestHeader HttpHeaders httpHeaders) throws IOException, InterruptedException {
        refactorService.resloveMS(requestItem.getServicesPath(), httpHeaders);
        return "CONGRATULATION! REFACTOR MEGA SERVICE SUCCESS!";
    }
    @PostMapping("/refactor/cs")
    public String resloveCS(@RequestBody RequestItem requestItem, @RequestHeader HttpHeaders httpHeaders) throws IOException, XmlPullParserException, ServiceException {
        refactorService.resloveCS(requestItem.getServicesPath(), httpHeaders);
        return "CONGRATULATION! REFACTOR CHATTY SERVICE SUCCESS!";
    }
    @PostMapping("/refactor/sc")
    public String resloveSC(@RequestBody RequestItem requestItem, @RequestHeader HttpHeaders httpHeaders) throws IOException, XmlPullParserException, ServiceException {
        refactorService.resloveSC(requestItem.getServicesPath(), httpHeaders);
        return "CONGRATULATION! REFACTOR SERVICE CHAIN SUCCESS!";
    }

    @PostMapping("/refactor/ns")
    public String resloveNS(@RequestBody RequestItem requestItem, @RequestHeader HttpHeaders httpHeaders) throws IOException, InterruptedException, ServiceException {
        refactorService.resloveNS(requestItem.getServicesPath(), httpHeaders);
        return "CONGRATULATION! REFACTOR NANO SERVICE SUCCESS!";
    }

    @PostMapping("/refactor/sp")
    public String resloveSP(@RequestBody RequestItem requestItem, @RequestHeader HttpHeaders httpHeaders) throws IOException {
        refactorService.resloveSP(requestItem.getServicesPath(), httpHeaders);
        return "CONGRATULATION! REFACTOR SHARING PERSISTENCE SUCCESS!";
    }

//    @PostMapping("/refactor/test")
//    public String resloveTEST(@RequestBody RequestItem requestItem, @RequestHeader HttpHeaders httpHeaders) throws IOException {
//        refactorService.resloveTEST(requestItem.getServicesPath(), httpHeaders);
//        return "CONGRATULATION! REFACTOR SHARING PERSISTENCE SUCCESS!";
//    }

    @PostMapping("/refactor/nav")
    public String resloveNAV(@RequestBody RequestItem requestItem, @RequestHeader HttpHeaders httpHeaders) throws IOException, XmlPullParserException, ParseException {
        refactorService.resloveNAV(requestItem.getServicesPath(), httpHeaders);
        return "CONGRATULATION! REFACTOR SHARING PERSISTENCE SUCCESS!";
    }

    @PostMapping("/refactor/test")
    public String resloveTest(@RequestBody RequestItem requestItem, @RequestHeader HttpHeaders httpHeaders) throws IOException, XmlPullParserException, ParseException {
        refactorService.resloveTest(requestItem, httpHeaders);
        return "CONGRATULATION! REFACTOR SHARING PERSISTENCE SUCCESS!";
    }

    @PostMapping("/refactor/nag")
    public String resolveNAG(@RequestBody RequestItem requestItem, @RequestHeader HttpHeaders httpHeaders) throws Exception {
        refactorService.resolveNAG(requestItem.getServicesPath(), httpHeaders);
        return "CONGRATULATION! REFACTOR NO API GATEWAY SUCCESS!";
    }

    @PostMapping("/refactor/nsdp")
    public String resloveNSDP(@RequestBody RequestItem requestItem, @RequestHeader HttpHeaders httpHeaders) throws IOException, XmlPullParserException {
        refactorService.resolveNSDP(requestItem.getServicesPath(), httpHeaders);
        return "CONGRATULATION! REFACTOR NANO SERVICE DISCOVERY PATTERN SUCCESS!";
    }

    @PostMapping("/refactor/us")
    public String resloveUS(@RequestBody RequestItem requestItem, @RequestHeader HttpHeaders httpHeaders) throws IOException, XmlPullParserException {
        refactorService.resolveUS(requestItem.getServicesPath(), httpHeaders);
        return "CONGRATULATION! REFACTOR UNNECESSARY SETTINGS SUCCESS!";
    }

    @PostMapping("/refactor/ebsi")
    public String resloveEBSI(@RequestBody RequestItem requestItem, @RequestHeader HttpHeaders httpHeaders) throws IOException, XmlPullParserException {
        refactorService.resolveEBSI(requestItem.getServicesPath(), httpHeaders);
        return "CONGRATULATION! REFACTOR ENDPOINT BASED SERVICE INTERACTION SUCCESS!";
    }
}
