package com.refactor.executor.agent;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.refactor.executor.pod.PodInfo;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

//import static com.refactor.executor.agent.K8sClusterAgent.getPodInfos;

/**
 * @description:
 * @author: xyc
 * @date: 2024-12-02 17:31
 */
public class NacosAgent {

//    public static void main(String[] args) throws NacosException, ApiException {
//
//
//        ApiClient client = new ApiClient();
//        client.setBasePath("https://10.245.1.233:6443"); // 集群的 API 地址
//        client.setApiKey("Bearer "+"eyJhbGciOiJSUzI1NiIsImtpZCI6Ik9oRUlMOXR3OHdFcjduUEx1VjFLTE94WlQ4Tkd2ZnRsX2NoM0REMEMzUWsifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJhcGlpbnZva2VyLXRva2VuIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6ImFwaWludm9rZXIiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiIwNzE1MjIwMi01OGNkLTQ4YTEtODk2Mi03YTk3YzkzMDg2NjkiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6a3ViZS1zeXN0ZW06YXBpaW52b2tlciJ9.LbjNXWHo76Y_A4JPU0qbTdjUlZ9tZDL76cXyE9Ap8ZSN6YHRzMLHReHaKnAI38uIWhlfCWITNTn6iLqtv8iMmeSpjbwi2FFfXo8MLmN_RmrAb529dXPrS8RPmdwlvZ2Y8MMqLW1VvvDH1mEF-uPxoeFi-G693G9cW3u_kwkTEnFBTBgab5-xPkAx0v9IJc8vYX31guUpdZ4bNpb3ijHglv4OM0H8OHuYlyBELOnGUpziIB39Vw_uDUblbjlSodRrnEGR7TC2ECqsXzc9d5pOtPS2o9hoj0LY12hjmKqAjhUrBLyhe1E2yKT65SG5zpsWmHgQPYoNqqOqdADVn3_UoQ");
////            client.setDebugging(true);
//        client.setVerifyingSsl(false);
//
//        Configuration.setDefaultApiClient(client);
//        // 将配置设置为默认的 API 客户端
//        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
//
//        // 创建 CoreV1Api 实例
//        CoreV1Api api = new CoreV1Api();
//        CustomObjectsApi customObjectsApi = new CustomObjectsApi(client);
//        Map<String, List<PodInfo>> servicePodDetails = getPodInfos(api);
//        labelNacosInstance(servicePodDetails);
////        String serverAddr = "10.245.1.233:8848";
////
////        // 服务名称
////        String serviceName = "test-service";
////
////        try {
////            // 创建 Properties 对象
////            Properties properties = new Properties();
////            properties.put("serverAddr", serverAddr);
////
////            // 创建 NamingService 实例
////            NamingService namingService = NamingFactory.createNamingService(properties);
////            List<ServiceInfo> serviceInfoList = namingService.getSubscribeServices();
////            System.out.println(serviceInfoList.size());
////            for (ServiceInfo serviceInfo: serviceInfoList)
////                System.out.println("name:" + serviceInfo.toString());
////            // 获取所有实例信息
////            List<Instance> instances = namingService.getAllInstances(serviceName);
////
////            // 打印实例信息
////            for (Instance instance : instances) {
////                System.out.println("Instance: " + instance);
////                Map<String, String> metadata = new HashMap<>();
////                metadata.put("node", "node1");
////                instance.setMetadata(metadata);
////                namingService.registerInstance(serviceName, instance);
////            }
////
////            List<Instance> instances2 = namingService.getAllInstances(serviceName);
////            for (Instance instance : instances) {
////                System.out.println("Instance: " + instance);
////            }
////        } catch (NacosException e) {
////            e.printStackTrace();
////        }
//    }
    public static String findPodNode(Map<String, List<PodInfo>> servicePodDetails,String ip){
        for (String service: servicePodDetails.keySet()){
            List<PodInfo> podInfoList = servicePodDetails.get(service);
            for (PodInfo podInfo: podInfoList){
                if (podInfo.getPodIp().equals(ip)){
                    return podInfo.getNodeName();
                }
            }
        }
        return null;
    }

    public static void labelNacosInstance(Map<String, List<PodInfo>> servicePodDetails){
        String serverAddr = "10.245.1.233:8848";

        // 服务名称
        String serviceName = "test-service";

        try {
            // 创建 Properties 对象
            Properties properties = new Properties();
            properties.put("serverAddr", serverAddr);

            // 创建 NamingService 实例
            NamingService namingService = NamingFactory.createNamingService(properties);
            List<ServiceInfo> serviceInfoList = namingService.getSubscribeServices();
            System.out.println(serviceInfoList.size());
            for (ServiceInfo serviceInfo: serviceInfoList)
                System.out.println("name:" + serviceInfo.toString());
            // 获取所有实例信息
            List<Instance> instances = namingService.getAllInstances(serviceName);

            // 打印实例信息
            for (Instance instance : instances) {
                String nodeName = findPodNode(servicePodDetails, instance.getIp());
                System.out.println("Instance: " + instance);
                Map<String, String> metadata = new HashMap<>();
                metadata.put("node", nodeName);
                instance.setMetadata(metadata);
                namingService.registerInstance(serviceName, instance);
            }

            List<Instance> instances2 = namingService.getAllInstances(serviceName);
            for (Instance instance : instances) {
                System.out.println("Instance: " + instance);
            }
        } catch (NacosException e) {
            e.printStackTrace();
        }

    }
}