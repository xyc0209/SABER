package com.refactor.executor.agent;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import com.refactor.executor.dto.ChainKey;
import com.refactor.executor.node.MNodesMetricsResponse;
import com.refactor.executor.node.NodeInfo;
import com.refactor.executor.pod.ContainersItem;
import com.refactor.executor.pod.MPodsMetricsResponse;
import com.refactor.executor.pod.PodInfo;
import com.refactor.executor.utils.CastUtils;
import io.kubernetes.client.common.KubernetesType;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
//import org.yaml.snakeyaml.Yaml;
import io.kubernetes.client.util.Yaml;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLOutput;
import java.util.*;

/**
 * @description:
 * @author: xyc
 * @date: 2024-09-28 16:12
 */
@Component
public class K8sClusterAgent {

    @Value("${k8s.namespace}")
    public  String nameSpace;

    @Value("${k8s.api}")
    public  String k8s_addr;
    @Value("${k8s.token}")
    public  String k8s_token;



    public static String deploySVC(String fileName){
        String yamlFilePath = fileName;

        try {
            // 创建 ProcessBuilder 并设置命令
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "kubectl", "apply", "-f", yamlFilePath);

            // 启动进程
            Process process = processBuilder.start();

            // 获取进程的输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // 等待进程完成
            int exitCode = process.waitFor();
            System.out.println("Kubernetes YAML execution completed with exit code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing Kubernetes YAML: " + e.getMessage());
            e.printStackTrace();
        }
        return "DEPLOY SUCCESS";
    }

    public  String deleteResourcesByBatch(Map<String, Map<String, String>> serviceDetails){
    List<String> svcList = new ArrayList<>();
        System.out.println("DELETE serviceDetails"+ serviceDetails.toString());
        for(Map.Entry<String, Map<String, String>> svcDetail: serviceDetails.entrySet()) {
            Map<String, String> detail = svcDetail.getValue();
            String serviceName = detail.get("serviceName");
            //删除mega service
            if (serviceName.contains("service-")) {
                svcList.add(serviceName.substring(0, serviceName.length() - 2));
                break;
            }
            else
                svcList.add(detail.get("serviceName"));
        }
        deleteResources(svcList, this.nameSpace);
        return "DELETE SUCCESS";
    }

    public static String deleteResources(List<String> resourceNames, String namespace) {
        StringBuilder result = new StringBuilder();

        for (String resourceName : resourceNames) {
            System.out.println("resourceName---"+ resourceName);
            System.out.println("namespace---"+ namespace);
            // 删除 Deployment
            try {
                // 创建 ProcessBuilder 并设置命令
                ProcessBuilder processBuilder = new ProcessBuilder(
                        "kubectl", "delete", "deployment", resourceName, "-n", namespace);

                // 启动进程
                Process process = processBuilder.start();

                // 获取进程的输出
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

                // 等待进程完成
                int exitCode = process.waitFor();
                System.out.println("Deletion of Deployment '" + resourceName + "' completed with exit code: " + exitCode);
                result.append("Deleted Deployment: ").append(resourceName).append("\n");

            } catch (IOException | InterruptedException e) {
                System.err.println("Error deleting Deployment '" + resourceName + "': " + e.getMessage());
                e.printStackTrace();
                result.append("Failed to delete Deployment: ").append(resourceName).append(" Error: ").append(e.getMessage()).append("\n");
            }

            // 删除 Service
            try {
                // 创建 ProcessBuilder 并设置命令
                ProcessBuilder processBuilder = new ProcessBuilder(
                        "kubectl", "delete", "service", resourceName, "-n", namespace);

                // 启动进程
                Process process = processBuilder.start();

                // 获取进程的输出
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

                // 等待进程完成
                int exitCode = process.waitFor();
                System.out.println("Deletion of Service '" + resourceName + "' completed with exit code: " + exitCode);
                result.append("Deleted Service: ").append(resourceName).append("\n");

            } catch (IOException | InterruptedException e) {
                System.err.println("Error deleting Service '" + resourceName + "': " + e.getMessage());
                e.printStackTrace();
                result.append("Failed to delete Service: ").append(resourceName).append(" Error: ").append(e.getMessage()).append("\n");
            }
        }

        return result.toString();
    }

    public static String createDeploymentByBatch(Map<String, Map<String, String>> serviceDetails) {
        System.out.println();
        for(Map.Entry<String, Map<String, String>> svcDetail: serviceDetails.entrySet()){
            Map<String, String> detail = svcDetail.getValue();
            System.out.println("IMAGE name:" + detail.get("imageName"));
            System.out.println("SERVICE NAME:" + detail.get("serviceName").toLowerCase());
            System.out.println("SERVICE PORT:"+ Integer.valueOf(detail.get("servicePort")));
            deploySVC(createDeploymentFromImage(detail.get("imageName"), detail.get("serviceName"), Integer.valueOf(detail.get("servicePort"))));
        }
        return "deploy success";
    }
    public static String createDeploymentFromImage(String imageName, String serviceName, Integer port) {
        try {
            // 初始化 Kubernetes API 客户端
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);

            // 创建 Deployment 资源
            V1Deployment deployment = new V1Deployment()
                    .apiVersion("apps/v1")
                    .kind("Deployment")
                    .metadata(new V1ObjectMeta()
                            .name(serviceName)
                            .namespace("kube-test")
                            .labels(new HashMap<String, String>() {{ put("app", serviceName); }}))
                    .spec(new V1DeploymentSpec()
                            .replicas(1)
                            .selector(new V1LabelSelector()
                                    .matchLabels(new HashMap<String, String>() {{ put("app", serviceName); }}))
                            .template(new V1PodTemplateSpec()
                                    .metadata(new V1ObjectMeta()
                                            .labels(new HashMap<String, String>() {{ put("app", serviceName); }}))
                                    .spec(new V1PodSpec()
                                            .containers(Arrays.asList(
                                                    new V1Container()
                                                            .name(serviceName)
                                                            .image(imageName)
                                                            .imagePullPolicy("Always")
                                                            .ports(Arrays.asList(
                                                                    new V1ContainerPort()
                                                                            .containerPort(port))))))));
            // 创建 Service 资源
            V1Service service = new V1Service()
                    .apiVersion("v1")
                    .kind("Service")
                    .metadata(new V1ObjectMeta()
                            .name(serviceName)
                            .namespace("kube-test")
                            .labels(Collections.singletonMap("app", serviceName)))
                    .spec(new V1ServiceSpec()
                            .ports(Arrays.asList(
                                    new V1ServicePort()
                                            .name("http")
                                            .port(port)
                                            .nodePort(generateRandomNodePort())))
                            .selector(Collections.singletonMap("app", serviceName))
                            .type("NodePort"));
            // 输出 Deployment 对象

            String yamlFile = serviceName + "_deployment.yaml";
            writeToYamlFileAll(yamlFile, deployment, service);

            return yamlFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int generateRandomNodePort() {
        int min = 30000;
        int max = 32767;
        return min + (int)(Math.random() * ((max - min) + 1));
    }


//    private static void writeToYamlFile(String fileName, String content) {
//        try (FileWriter writer = new FileWriter(fileName)) {
//            writer.write(content);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private static void writeToYamlFile(String fileName, V1Deployment deployment) {
        try (FileWriter writer = new FileWriter(fileName)) {
            Yaml yaml = new Yaml();
            writer.write(yaml.dump(deployment));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void writeToYamlFileAll(String fileName, V1Deployment deployment, V1Service service) {
        try (FileWriter writer = new FileWriter(fileName)) {
            Yaml.dump(deployment, writer);
            writer.write("---\n"); // 添加分隔符
            Yaml.dump(service, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeToYamlFileAll(String fileName, List<V1Deployment> deploymentList, V1Service service) {
        try (FileWriter writer = new FileWriter(fileName)) {
            Yaml yaml = new Yaml();
            List<KubernetesType> objects = new ArrayList<>();
            for (V1Deployment deployment: deploymentList){
                objects.add(deployment);
            }
            objects.add(service);
            yaml.dumpAll(objects.iterator(), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String createDeploymentByBatch(Map<String, Map<String, String>> serviceDetails, Map<Integer, Set<String>> deployDetail) {
        System.out.println();
        for(Map.Entry<String, Map<String, String>> svcDetail: serviceDetails.entrySet()){
            Map<String, String> detail = svcDetail.getValue();
            System.out.println("IMAGE name:" + detail.get("imageName"));
            System.out.println("SERVICE NAME:" + detail.get("serviceName").toLowerCase());
            System.out.println("SERVICE PORT:"+ Integer.valueOf(detail.get("servicePort")));
            deploySVC(createCSDeploymentFromImage(detail.get("imageName"), detail.get("serviceName"), Integer.valueOf(detail.get("servicePort")), deployDetail));
        }
        return "deploy success";
    }

    public static String createCSDeploymentFromImage(String imageName, String serviceName, Integer port, Map<Integer, Set<String>> deployDetail) {
        try {
            // 初始化 Kubernetes API 客户端
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);

            // 创建一个列表来存储所有 Kubernetes 对象
            List<V1Deployment> deploymentList = new ArrayList<>();

            // 创建 Service 资源
            V1Service service = new V1Service()
                    .apiVersion("v1")
                    .kind("Service")
                    .metadata(new V1ObjectMeta()
                            .name(serviceName)  // 为 Service 使用统一的名称
                            .namespace("kube-test")
                            .labels(Collections.singletonMap("app", serviceName)))
                    .spec(new V1ServiceSpec()
                            .ports(Arrays.asList(
                                    new V1ServicePort()
                                            .name("http")
                                            .port(port)
                                            .nodePort(generateRandomNodePort())))
                            .selector(Collections.singletonMap("app", serviceName))  // 使用相同标签选择所有 Pod
                            .type("NodePort"));


            // 遍历 deployDetail 中的每个条目
            for (Map.Entry<Integer, Set<String>> entry : deployDetail.entrySet()) {
                Integer replicas = entry.getKey();  // Pod 的副本数
                Set<String> allowedNodes = entry.getValue();  // 允许部署的节点集合

                // 创建 Node Selector Term
                V1NodeSelectorTerm nodeSelectorTerm = new V1NodeSelectorTerm()
                        .matchExpressions(Collections.singletonList(
                                new V1NodeSelectorRequirement()
                                        .key("kubernetes.io/hostname")
                                        .operator("In")  // 使用 "In" 操作符来匹配
                                        .values(new ArrayList<>(allowedNodes))
                        ));

                // 创建 Deployment 资源
                V1Deployment deployment = new V1Deployment()
                        .apiVersion("apps/v1")
                        .kind("Deployment")
                        .metadata(new V1ObjectMeta()
                                .name(serviceName + "-" + replicas)  // 为每个 Deployment 使用不同的名称
                                .namespace("kube-test")
                                .labels(Collections.singletonMap("app", serviceName)))  // 使用相同标签
                        .spec(new V1DeploymentSpec()
                                .replicas(replicas)  // 使用传入的副本数
                                .selector(new V1LabelSelector()
                                        .matchLabels(Collections.singletonMap("app", serviceName)))  // 使用相同标签选择器
                                .template(new V1PodTemplateSpec()
                                        .metadata(new V1ObjectMeta()
                                                .labels(Collections.singletonMap("app", serviceName)))  // 使用相同标签
                                        .spec(new V1PodSpec()
                                                .affinity(new V1Affinity()  // 设置 affinity
                                                        .nodeAffinity(new V1NodeAffinity()
                                                                .requiredDuringSchedulingIgnoredDuringExecution(new V1NodeSelector()
                                                                        .nodeSelectorTerms(Collections.singletonList(nodeSelectorTerm)))
                                                        )
                                                )
                                                .containers(Arrays.asList(
                                                        new V1Container()
                                                                .name(serviceName)
                                                                .image(imageName)
                                                                .imagePullPolicy("Always")
                                                                .ports(Arrays.asList(
                                                                        new V1ContainerPort()
                                                                                .containerPort(port))
                                                                )
                                                )))));

                // 将 Deployment 添加到资源列表
                deploymentList.add(deployment);
            }

            // 将所有 Kubernetes 对象写入同一个 YAML 文件
            String yamlFile = serviceName + "_resources.yaml";
            writeToYamlFileAll(yamlFile, deploymentList, service);

            // 返回 YAML 文件名
            return yamlFile;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public  Map<String, List<PodInfo>> getPodsADetails() throws ApiException {
        ApiClient client = new ApiClient();
        client.setBasePath("https://10.245.1.233:6443"); // 集群的 API 地址
        client.setApiKey("Bearer "+"eyJhbGciOiJSUzI1NiIsImtpZCI6Ik9oRUlMOXR3OHdFcjduUEx1VjFLTE94WlQ4Tkd2ZnRsX2NoM0REMEMzUWsifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJhcGlpbnZva2VyLXRva2VuIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6ImFwaWludm9rZXIiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiIwNzE1MjIwMi01OGNkLTQ4YTEtODk2Mi03YTk3YzkzMDg2NjkiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6a3ViZS1zeXN0ZW06YXBpaW52b2tlciJ9.LbjNXWHo76Y_A4JPU0qbTdjUlZ9tZDL76cXyE9Ap8ZSN6YHRzMLHReHaKnAI38uIWhlfCWITNTn6iLqtv8iMmeSpjbwi2FFfXo8MLmN_RmrAb529dXPrS8RPmdwlvZ2Y8MMqLW1VvvDH1mEF-uPxoeFi-G693G9cW3u_kwkTEnFBTBgab5-xPkAx0v9IJc8vYX31guUpdZ4bNpb3ijHglv4OM0H8OHuYlyBELOnGUpziIB39Vw_uDUblbjlSodRrnEGR7TC2ECqsXzc9d5pOtPS2o9hoj0LY12hjmKqAjhUrBLyhe1E2yKT65SG5zpsWmHgQPYoNqqOqdADVn3_UoQ");
//            client.setDebugging(true);
        client.setVerifyingSsl(false);

        Configuration.setDefaultApiClient(client);
        // 将配置设置为默认的 API 客户端
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);

        // 创建 CoreV1Api 实例
        CoreV1Api api = new CoreV1Api();
        CustomObjectsApi customObjectsApi = new CustomObjectsApi(client);
        Map<String, List<PodInfo>> servicePodDetails =getPodInfos(api);
        getContainerInfos(customObjectsApi, servicePodDetails);
        return servicePodDetails;
    }

    public  ApiClient getClient(){
        ApiClient client = new ApiClient();
        System.out.println("k8s_addr" +this.k8s_addr);
        client.setBasePath(k8s_addr); // 集群的 API 地址
        client.setApiKey("Bearer "+ k8s_token);
//            client.setDebugging(true);
        client.setVerifyingSsl(false);

        Configuration.setDefaultApiClient(client);
        // 将配置设置为默认的 API 客户端
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
        return client;
    }

    public  void createDeploymentSCByBatch(List<ChainKey> chainKeyList, Map<String, Map<String, String>> serviceDetails) throws ApiException {
        ApiClient client = getClient();
        // 创建 CoreV1Api 实例
        CoreV1Api api = new CoreV1Api();
        CustomObjectsApi customObjectsApi = new CustomObjectsApi(client);
        Map<String, List<PodInfo>> servicePodDetails =getPodInfos(api);
        getContainerInfos(customObjectsApi, servicePodDetails);
        Map<String, NodeInfo> nodeInfoMap = getNodeInfos(api, customObjectsApi);
        System.out.println("nodeInfoMap" +nodeInfoMap.toString());
        Map<String, Map<String, Integer>> depoyDetail = new HashMap<>();
        //记录新部署的服务及对应的节点集合
        Map<String, Set<String>> svc2Nodes = new HashMap<>();
        Set<String> deployedServiceSet = new HashSet<>();
        Set<String> precursorNodeList;
        Set<String> nodeList;
        for (ChainKey key: chainKeyList){
            List<String> chain = key.getChain();
            precursorNodeList = new HashSet<>();
            for (int i=0; i< chain.size(); i++){
                String service = chain.get(i);
                //记录前驱服务部署的nodeList
                if (!deployedServiceSet.contains(service) && i != 0) {
                    nodeList = precursorNodeList.isEmpty() ? (svc2Nodes.containsKey(chain.get(i-1))? svc2Nodes.get(chain.get(i-1)) : getNodelist(servicePodDetails, chain.get(i-1))) : precursorNodeList;
                    Map<String, BigDecimal> availableMap = getNodeAvailableResource(nodeInfoMap, nodeList);
                    Map<Integer, Set<String>> deployDetail = deployToNode(serviceDetails.get(service), servicePodDetails, nodeInfoMap, nodeList, service, availableMap, precursorNodeList, svc2Nodes);
                    System.out.println("Processing service: " + service);
                    deployedServiceSet.add(service);
                    createDeploymentByBatch(serviceDetails, deployDetail);
                }
            }
        }
    }
    public  void createDeploymentCSByBatch(List<Set<String>> csSetList,Map<String, Map<String, String>> serviceDetails) throws ApiException {
        ApiClient client = getClient();
        // 创建 CoreV1Api 实例
        CoreV1Api api = new CoreV1Api();
        CustomObjectsApi customObjectsApi = new CustomObjectsApi(client);
        Map<String, List<PodInfo>> servicePodDetails =getPodInfos(api);
        getContainerInfos(customObjectsApi, servicePodDetails);

        Map<String, NodeInfo> nodeInfoMap = getNodeInfos(api, customObjectsApi);
        Map<String, Set<String>> svc2Nodes = new HashMap<>();
        System.out.println("nodeInfoMap" +nodeInfoMap.toString());
        for (Set<String> csSet: csSetList){
            Iterator<String> iterator = csSet.iterator();
            if (iterator.hasNext()) {
                String firstService = iterator.next(); // 获取第一个service
                //获取第一个服务所在的node集合，计算资源部署第二个服务，然后
                System.out.println("First value: " + firstService);
                Set<String> nodeList = getNodelist(servicePodDetails, firstService);
                Map<String,BigDecimal> availableMap = getNodeAvailableResource(nodeInfoMap, nodeList);
                // 从第二个值开始遍历
                while (iterator.hasNext()) {
                    String service = iterator.next();
                    Map<Integer,Set<String>> deployDetail = deployToNode(serviceDetails.get(service),servicePodDetails, nodeInfoMap, nodeList, service, availableMap, null, svc2Nodes);
                    System.out.println("Processing service: " + service);
                    createDeploymentByBatch(serviceDetails, deployDetail);
                }
            }
        }
    }
    public static Map<Integer,Set<String>> deployToNode(Map<String, String> serviceDetail, Map<String, List<PodInfo>> servicePodDetails, Map<String, NodeInfo> nodeInfoMap, Set<String> nodeList, String service, Map<String, BigDecimal> availableMap, Set<String> precursorNodeList, Map<String, Set<String>> svc2Nodes ){
        int result;
        Map<Integer,Set<String>> deployDetail = new HashMap<>();
        Map<String,BigDecimal> resourceMap = getAllResource(servicePodDetails,service);
        BigDecimal availableCpu  = availableMap.get("cpu");
        BigDecimal availableRam = availableMap.get("ram");
        BigDecimal consumedCpu  = resourceMap.get("cpu");
        BigDecimal consumedRam  = resourceMap.get("ram");
        if(!serviceDetail.containsKey("cpuRequest")) {
            if (availableCpu.compareTo(consumedCpu) > 0 && availableRam.compareTo(consumedRam) > 0) {
                availableMap.put("cpu", availableCpu.subtract(consumedCpu));
                availableMap.put("ram", availableRam.subtract(consumedRam));
                deployDetail.put(servicePodDetails.get(service).size(), nodeList);
                svc2Nodes.putIfAbsent(service, nodeList);
                if (precursorNodeList != null)
                    precursorNodeList = nodeList;
                updateNodeInfoMap(nodeInfoMap, nodeList, consumedCpu, consumedRam);
            }
            else{
                int count = servicePodDetails.get(service).size();
                BigDecimal avgCpu = consumedCpu.divide(BigDecimal.valueOf(count));
                BigDecimal avgRam = consumedRam.divide(BigDecimal.valueOf(count));
                result = Math.min(availableCpu.divide(avgCpu, RoundingMode.FLOOR).intValue(), availableRam.divide(avgRam, RoundingMode.FLOOR).intValue());
                //资源满足的实例部署在当前节点集合
                deployDetail.put(result, nodeList);
                updateNodeInfoMap(nodeInfoMap, nodeList, avgCpu.multiply(BigDecimal.valueOf(result)), avgRam.multiply(BigDecimal.valueOf(result)));
                //剩余实例部署在其他节点上
                int otherCount = servicePodDetails.get(service).size() - result;
                Set<String> otherNodes = getOtherNodes(nodeInfoMap, nodeList);
                deployDetail.put(otherCount, otherNodes);
                svc2Nodes.putIfAbsent(service, otherNodes);
                if (precursorNodeList != null)
                    precursorNodeList = otherNodes;
                updateNodeInfoMap(nodeInfoMap, otherNodes, avgCpu.multiply(BigDecimal.valueOf(otherCount)), avgRam.multiply(BigDecimal.valueOf(otherCount)));
            }
        }
        else {
            //如果服务源码中有k8s部署yaml文件且有资源限制，则根据resources limit值判断
            String cpu = serviceDetail.get("cpuRequest");
            BigDecimal cpuRequest = new BigDecimal(cpu.substring(0, cpu.length() - 1));
            String memory = serviceDetail.get("memoryRequest");
            BigDecimal memoryRequest = new BigDecimal(memory.substring(0, memory.length() - 1));
            int svcCount = servicePodDetails.get(service).size();
            result = Math.min(availableCpu.divide(cpuRequest, RoundingMode.FLOOR).intValue(), availableRam.divide(memoryRequest, RoundingMode.FLOOR).intValue());
            if(result >=svcCount) {
                availableMap.put("cpu", availableCpu.subtract(cpuRequest.multiply(BigDecimal.valueOf(svcCount))));
                availableMap.put("ram", availableRam.subtract(memoryRequest.multiply(BigDecimal.valueOf(svcCount))));
                //资源满足的实例部署在当前节点集合
                if (precursorNodeList != null)
                    precursorNodeList = nodeList;
                deployDetail.put(svcCount, nodeList);
                svc2Nodes.putIfAbsent(service, nodeList);
                updateNodeInfoMap(nodeInfoMap, nodeList, cpuRequest.multiply(BigDecimal.valueOf(svcCount)), memoryRequest.multiply(BigDecimal.valueOf(svcCount)));
            }
            else{
                //资源满足的实例部署在当前节点集合
                deployDetail.put(result, nodeList);
                updateNodeInfoMap(nodeInfoMap, nodeList, cpuRequest.multiply(BigDecimal.valueOf(result)), memoryRequest.multiply(BigDecimal.valueOf(result)));
                //剩余实例部署在其他节点上
                Set<String> otherNodes = getOtherNodes(nodeInfoMap, nodeList);
                deployDetail.put(svcCount - result, otherNodes);
                svc2Nodes.putIfAbsent(service, otherNodes);
                if (precursorNodeList != null)
                    precursorNodeList = otherNodes;
                updateNodeInfoMap(nodeInfoMap, otherNodes, cpuRequest.multiply(BigDecimal.valueOf(svcCount - result)), memoryRequest.multiply(BigDecimal.valueOf(svcCount - result)));
            }
        }

        return deployDetail;
    }

    public static void updateNodeInfoMap(Map<String, NodeInfo> nodeInfoMap, Set<String> nodeList, BigDecimal cpuConsumed, BigDecimal memConsumed){
        int count = nodeList.size();
        BigDecimal avgCPUConsumed = cpuConsumed.divide(BigDecimal.valueOf(count));
        BigDecimal avgMemConsumed = memConsumed.divide(BigDecimal.valueOf(count));
        for (String node: nodeList){
            NodeInfo targetNode = nodeInfoMap.get(node);
            targetNode.setCpuUsed(avgCPUConsumed);
            targetNode.setRamUsed(avgMemConsumed);
        }

    }
    public static Set<String> getOtherNodes(Map<String, NodeInfo> nodeInfoMap, Set<String> nodeList){
        Set<String> otherNodes = new HashSet<>();
        for (Map.Entry<String, NodeInfo> entry: nodeInfoMap.entrySet()){
            String nodeMame = entry.getValue().getName();
            if(!nodeList.contains(nodeMame)){
                otherNodes.add(nodeMame);
            }
        }
        return otherNodes;
    }
    public static Map<String,BigDecimal> getNodeAvailableResource( Map<String, NodeInfo> nodeInfoMap,  Set<String> nodeList){
        Map<String,BigDecimal> availableMap = new HashMap<>();
        BigDecimal availableCpu = new BigDecimal(0);
        BigDecimal availableRam = new BigDecimal(0);
        for (Map.Entry<String, NodeInfo> entry: nodeInfoMap.entrySet()){
            if (nodeList.contains(entry.getKey())) {
                availableCpu = availableCpu.add(entry.getValue().getCpuMax().multiply(new BigDecimal(0.8)).subtract(entry.getValue().getCpuUsed()));
                availableRam = availableRam.add(entry.getValue().getRamMAX().multiply(new BigDecimal(0.8)).subtract(entry.getValue().getRamUsed()));
            }
        }
        availableMap.put("cpu",availableCpu);
        availableMap.put("ram", availableRam);
        return availableMap;
    }
    public static Map<String,BigDecimal> getAllResource(Map<String, List<PodInfo>> servicePodDetails, String service){
        Map<String,BigDecimal> resourceMap = new HashMap<>();
        List<PodInfo> podInfoList = servicePodDetails.get(service);
        BigDecimal cpuUsed = new BigDecimal(0);
        BigDecimal ramUsed = new BigDecimal(0);
        for (PodInfo podInfo: podInfoList){
            for (ContainersItem containersItem: podInfo.getContainers()){
                String cpu = containersItem.getUsage().getCpu();
                String ram = containersItem.getUsage().getMemory();
                cpuUsed = cpuUsed.add(new BigDecimal(cpu.substring(0, cpu.length() -1)));
                ramUsed = ramUsed.add(new BigDecimal(ram.substring(0, ram.length() -2)));
            }
        }
        resourceMap.put("cpu",cpuUsed);
        resourceMap.put("ram", ramUsed);
        return resourceMap;

    }
        public static Set<String> getNodelist(Map<String, List<PodInfo>> servicePodDetails, String service){
        Set<String>  nodeList = new HashSet<>();
            List<PodInfo> podInfoList = servicePodDetails.get(service);
            for (PodInfo podInfo: podInfoList){
                nodeList.add(podInfo.getNodeName());
            }
            return nodeList;

        }

        public static void getContainerInfos(CustomObjectsApi customObjectsApi, Map<String, List<PodInfo>> servicePodDetails) throws ApiException {
            // 创建 CustomObjectsApi 实例

            // 获取 Pod 资源使用情况
            CustomObjectsApi.APIlistNamespacedCustomObjectRequest podMetrics = customObjectsApi.listNamespacedCustomObject(
                    "metrics.k8s.io",
                    "v1beta1",
                    "kube-test", // 替换为您的命名空间
                    "pods"
            );
            Gson gson = new Gson();
            System.out.println(podMetrics.execute().toString());
            MPodsMetricsResponse metrics = gson.fromJson( gson.toJson(podMetrics.execute()), MPodsMetricsResponse.class);

            System.out.println(podMetrics.execute().toString());
            // 打印 Pod 资源使用情况
            metrics.getItems().forEach(metric -> {
                System.out.println("Pod: " + metric.getMetadata().getName());
                String podName = metric.getMetadata().getName();

                servicePodDetails.get(getServiceName(podName)).forEach(podInfo -> {
                    System.out.println("podName: "+podName);
                    if (podInfo.getName().equals(podName)){
                        podInfo.setContainers(metric.getContainers());
                        System.out.println("podInfo" +podInfo.toString());
                    }
                });
            });

        }

        public static void testResource() throws IOException, ApiException {
        // 创建 Kubernetes API 客户端
        ApiClient client = Config.defaultClient();
        client.setBasePath("https://10.245.1.233:6443"); // 集群的 API 地址
        client.setApiKey("Bearer "+"eyJhbGciOiJSUzI1NiIsImtpZCI6Ik9oRUlMOXR3OHdFcjduUEx1VjFLTE94WlQ4Tkd2ZnRsX2NoM0REMEMzUWsifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJhcGlpbnZva2VyLXRva2VuIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6ImFwaWludm9rZXIiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiIwNzE1MjIwMi01OGNkLTQ4YTEtODk2Mi03YTk3YzkzMDg2NjkiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6a3ViZS1zeXN0ZW06YXBpaW52b2tlciJ9.LbjNXWHo76Y_A4JPU0qbTdjUlZ9tZDL76cXyE9Ap8ZSN6YHRzMLHReHaKnAI38uIWhlfCWITNTn6iLqtv8iMmeSpjbwi2FFfXo8MLmN_RmrAb529dXPrS8RPmdwlvZ2Y8MMqLW1VvvDH1mEF-uPxoeFi-G693G9cW3u_kwkTEnFBTBgab5-xPkAx0v9IJc8vYX31guUpdZ4bNpb3ijHglv4OM0H8OHuYlyBELOnGUpziIB39Vw_uDUblbjlSodRrnEGR7TC2ECqsXzc9d5pOtPS2o9hoj0LY12hjmKqAjhUrBLyhe1E2yKT65SG5zpsWmHgQPYoNqqOqdADVn3_UoQ");
//            client.setDebugging(true);
        client.setVerifyingSsl(false);
        Configuration.setDefaultApiClient(client);

        // 创建 CustomObjectsApi 实例
        CustomObjectsApi customObjectsApi = new CustomObjectsApi(client);

        // 获取 Pod 资源使用情况
        CustomObjectsApi.APIlistNamespacedCustomObjectRequest podMetrics = customObjectsApi.listNamespacedCustomObject(
                "metrics.k8s.io",
                "v1beta1",
                "kube-test", // 替换为您的命名空间
                "pods"
        );
        Gson gson = new Gson();
        System.out.println(podMetrics.execute().toString());
        MPodsMetricsResponse metrics = gson.fromJson( gson.toJson(podMetrics.execute()), MPodsMetricsResponse.class);

        System.out.println(podMetrics.execute().toString());
        // 打印 Pod 资源使用情况
        metrics.getItems().forEach(metric -> {
            System.out.println("Pod: " + metric.getMetadata().getName());
            metric.getContainers().forEach(container -> {
                System.out.println("Container: " + container.getName());
                System.out.println("CPU Usage: " + container.getUsage().getMemory());
                System.out.println("Memory Usage: " + container.getUsage().getCpu());
            });
        });
    }

    public static Map<String, List<PodInfo>> getPodInfos(CoreV1Api api) throws ApiException {
        CoreV1Api.APIlistPodForAllNamespacesRequest list = api.listPodForAllNamespaces();
        V1PodList execute = list.execute();
        // 遍历并打印每个 Pod 的名称和命名空间
        Map<String, List<PodInfo>> servicePodDetails = new HashMap<>();
        execute.getItems().forEach(v1Pod -> {
            V1ObjectMeta v1ObjectMeta= v1Pod.getMetadata();
            V1PodStatus v1PodStatus = v1Pod.getStatus();
            String podName = v1ObjectMeta.getName();
            String serviceName = getServiceName(podName);
            System.out.println("serviceName"+serviceName);
            if(servicePodDetails.get(serviceName) == null)
                servicePodDetails.put(serviceName, new ArrayList<>());
            PodInfo podInfo = new PodInfo();
            podInfo.setName(podName);
            podInfo.setNameSpace(v1ObjectMeta.getNamespace());
            podInfo.setPodIp(v1PodStatus.getPodIP());
            podInfo.setNodeName(v1Pod.getSpec().getNodeName());

            servicePodDetails.get(serviceName).add(podInfo);

            System.out.println(v1Pod.getMetadata().getName());
            System.out.println(v1Pod.getMetadata().getNamespace());
            System.out.println(v1Pod.getStatus().getPodIP());
            System.out.println(v1Pod.getSpec().getNodeName());
//                System.out.println(v1Pod.toJson());
        });
        System.out.println("servicePodDetails" +servicePodDetails.toString());
        return servicePodDetails;
    }

    public static Map<String, NodeInfo> getNodeInfos(CoreV1Api api, CustomObjectsApi metricsApi) throws ApiException {
        Map<String, NodeInfo> nodeInfoMap = new HashMap<>();
        V1NodeList nodeList = api.listNode().execute();
        System.out.println("NODE"+nodeList.toJson());


        CustomObjectsApi.APIlistClusterCustomObjectRequest nodeRequest = metricsApi.listClusterCustomObject(  "metrics.k8s.io",
                "v1beta1","nodes");
        Gson gson = new Gson();
        MNodesMetricsResponse nodeMetrics = gson.fromJson( gson.toJson(nodeRequest.execute()), MNodesMetricsResponse.class);;
        BigDecimal multiplier = new BigDecimal("1000000000"); // 1,000,000,000

        nodeList.getItems().forEach(v1Node -> {
            String nodeName = v1Node.getMetadata().getName();
            NodeInfo nodeInfo = new NodeInfo(nodeName);
            if(nodeInfoMap.get(nodeName) == null)
                nodeInfoMap.put(nodeName, nodeInfo);
            // 获取节点的总资源
            BigDecimal totalCpu = v1Node.getStatus().getCapacity().get("cpu").getNumber();
            BigDecimal totalMemory = v1Node.getStatus().getCapacity().get("memory").getNumber();
            nodeInfo.setCpuMax(totalCpu.multiply(multiplier));
            nodeInfo.setRamMAX(totalMemory);
            System.out.println("totalCpu" +totalCpu);
            System.out.println("totalMemory" +totalMemory);
        });
        nodeMetrics.getItems().forEach(node ->{
            System.out.println(node.getMetadata().getName());
            NodeInfo nodeInfo =nodeInfoMap.get(node.getMetadata().getName());
            BigDecimal  cpuUsed = CastUtils.convertCpuStringToBigDecimal(node.getUsage().getCpu());
            BigDecimal  memoryUsed = CastUtils.convertMemoryStringToBytes(node.getUsage().getMemory());
            nodeInfo.setCpuUsed(cpuUsed);
            nodeInfo.setRamUsed(memoryUsed);
            System.out.println(cpuUsed);
            System.out.println(memoryUsed);
        });
        return nodeInfoMap;
    }

    public static void main(String[] args) throws IOException, ApiException {
//        createDeploymentFromImage("10.245.1.233:5000/testreposity/test-service-3:refactor_1","tesstService", 8090);
////        createDeploymentCSByBatch(new ArrayList<>());
//        testResource();
////
//
//
            // 加载默认的 kubeconfig 文件
            ApiClient client = new ApiClient();
            client.setBasePath("https://10.245.1.233:6443"); // 集群的 API 地址
            client.setApiKey("Bearer "+"eyJhbGciOiJSUzI1NiIsImtpZCI6Ik9oRUlMOXR3OHdFcjduUEx1VjFLTE94WlQ4Tkd2ZnRsX2NoM0REMEMzUWsifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJhcGlpbnZva2VyLXRva2VuIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6ImFwaWludm9rZXIiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiIwNzE1MjIwMi01OGNkLTQ4YTEtODk2Mi03YTk3YzkzMDg2NjkiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6a3ViZS1zeXN0ZW06YXBpaW52b2tlciJ9.LbjNXWHo76Y_A4JPU0qbTdjUlZ9tZDL76cXyE9Ap8ZSN6YHRzMLHReHaKnAI38uIWhlfCWITNTn6iLqtv8iMmeSpjbwi2FFfXo8MLmN_RmrAb529dXPrS8RPmdwlvZ2Y8MMqLW1VvvDH1mEF-uPxoeFi-G693G9cW3u_kwkTEnFBTBgab5-xPkAx0v9IJc8vYX31guUpdZ4bNpb3ijHglv4OM0H8OHuYlyBELOnGUpziIB39Vw_uDUblbjlSodRrnEGR7TC2ECqsXzc9d5pOtPS2o9hoj0LY12hjmKqAjhUrBLyhe1E2yKT65SG5zpsWmHgQPYoNqqOqdADVn3_UoQ");
//            client.setDebugging(true);
            client.setVerifyingSsl(false);

            Configuration.setDefaultApiClient(client);
            // 将配置设置为默认的 API 客户端
            io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);

            // 创建 CoreV1Api 实例
            CoreV1Api api = new CoreV1Api();

            V1NodeList nodeList = api.listNode().execute();
            System.out.println("NODE"+nodeList.toJson());

            // 创建 CustomObjectsApi 实例用于获取 Metrics
            CustomObjectsApi metricsApi = new CustomObjectsApi();
            CustomObjectsApi.APIlistClusterCustomObjectRequest nodeRequest = metricsApi.listClusterCustomObject(  "metrics.k8s.io",
                    "v1beta1","nodes");
            Gson gson = new Gson();
            MNodesMetricsResponse nodeMetrics = gson.fromJson( gson.toJson(nodeRequest.execute()), MNodesMetricsResponse.class);;
            nodeList.getItems().forEach(v1Node -> {
                String nodeName = v1Node.getMetadata().getName();
                // 获取节点的总资源
                BigDecimal totalCpu = v1Node.getStatus().getCapacity().get("cpu").getNumber();
                BigDecimal totalMemory = v1Node.getStatus().getCapacity().get("memory").getNumber();
                System.out.println("totalCpu" +totalCpu);
                System.out.println("totalMemory" +totalMemory);
            });
//            nodeMetrics.getItems().forEach(node ->{
//                System.out.println(node.getMetadata().getName());
//                BigDecimal  cpuUsed = CastUtils.convertCpuStringToBigDecimal(node.getUsage().getCpu());
//                BigDecimal  memoryUsed = CastUtils.convertMemoryStringToBytes(node.getUsage().getMemory());
//                System.out.println(cpuUsed);
//                System.out.println(memoryUsed);
//            });
//
//            // 获取所有命名空间中的 Pod 列表
//            CoreV1Api.APIlistPodForAllNamespacesRequest list = api.listPodForAllNamespaces();
//            V1PodList execute = list.execute();
            // 遍历并打印每个 Pod 的名称和命名空间

//            Map<String, List<PodInfo>> servicePodDetails = new HashMap<>();
//
//            execute.getItems().forEach(v1Pod -> {
//                V1ObjectMeta v1ObjectMeta= v1Pod.getMetadata();
//                V1PodStatus v1PodStatus = v1Pod.getStatus();
//                String podName = v1ObjectMeta.getName();
//                String serviceName = getServiceName(podName);
//                System.out.println("serviceName"+serviceName);
//                if(servicePodDetails.get(serviceName) == null)
//                    servicePodDetails.put(serviceName, new ArrayList<>());
//                PodInfo podInfo = new PodInfo();
//                podInfo.setName(podName);
//                podInfo.setNameSpace(v1ObjectMeta.getNamespace());
//                podInfo.setPodIp(v1PodStatus.getPodIP());
//                podInfo.setNodeName(v1Pod.getSpec().getNodeName());
//
//                servicePodDetails.get(serviceName).add(podInfo);
//
//                System.out.println(v1Pod.getMetadata().getName());
//                System.out.println(v1Pod.getMetadata().getNamespace());
//                System.out.println(v1Pod.getStatus().getPodIP());
//                System.out.println(v1Pod.getSpec().getNodeName());
////                System.out.println(v1Pod.toJson());
//            });
//            System.out.println("servicePodDetails" +servicePodDetails.toString());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//// 创建 Kubernetes API 客户端
//        ApiClient client = Config.defaultClient();
//        client.setBasePath("https://10.245.1.233:6443");
//
//        // 设置Bearer Token
//        client.setApiKey("Bearer "+"eyJhbGciOiJSUzI1NiIsImtpZCI6Ik9oRUlMOXR3OHdFcjduUEx1VjFLTE94WlQ4Tkd2ZnRsX2NoM0REMEMzUWsifQ.eyJhdWQiOlsiaHR0cHM6Ly9rdWJlcm5ldGVzLmRlZmF1bHQuc3ZjLmNsdXN0ZXIubG9jYWwiXSwiZXhwIjoxNzMzMDI1Njk4LCJpYXQiOjE3MzMwMjIwOTgsImlzcyI6Imh0dHBzOi8va3ViZXJuZXRlcy5kZWZhdWx0LnN2Yy5jbHVzdGVyLmxvY2FsIiwianRpIjoiY2VhZjUxMGUtYTZhNy00ZjcxLTk5NGItYTk1ZWIxN2MzNzY4Iiwia3ViZXJuZXRlcy5pbyI6eyJuYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsInNlcnZpY2VhY2NvdW50Ijp7Im5hbWUiOiJhcGlpbnZva2VyIiwidWlkIjoiMDcxNTIyMDItNThjZC00OGExLTg5NjItN2E5N2M5MzA4NjY5In19LCJuYmYiOjE3MzMwMjIwOTgsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTphcGlpbnZva2VyIn0.lAYY0f1JhJ6Uok8ea67P073c219i9B6l71IQpu0_WDuv3uZY-TKLQqvSM-lwayNnAMq9czefnQ-atcjxtYn4CqlSvryqRAr8GS1fVYxTRoAa1Zq312YD0SB9FKh6idqxCcSxFGpq2rnYtzrSnR94zvQ3aYy3XpZJJPtDnThf0eU6d36szwXrJ26qX_EvdAlsKuoS-L3sl_UURPbYfvWv0dyJaRgm8UKAXhK1xA267ab4PHSOnvmmfOro_ANSk-oMkOLe9Z9hyMSM3kYekMo9LAx6XljfZ0_whfCcVwy1mYSNW8-WVJLUnCtyKpfrdbA8BECj1wRIjK8Ws91q1Bf8Aw");
////        String token = "Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6Ik9oRUlMOXR3OHdFcjduUEx1VjFLTE94WlQ4Tkd2ZnRsX2NoM0REMEMzUWsifQ.eyJhdWQiOlsiaHR0cHM6Ly9rdWJlcm5ldGVzLmRlZmF1bHQuc3ZjLmNsdXN0ZXIubG9jYWwiXSwiZXhwIjoxNzMzMDIyODcxLCJpYXQiOjE3MzMwMTkyNzEsImlzcyI6Imh0dHBzOi8va3ViZXJuZXRlcy5kZWZhdWx0LnN2Yy5jbHVzdGVyLmxvY2FsIiwianRpIjoiYmY2NDA3ZWMtZmI5NC00YjBmLTllMzEtMDdkMTRlMWVhMjAyIiwia3ViZXJuZXRlcy5pbyI6eyJuYW1lc3BhY2UiOiJkZWZhdWx0Iiwic2VydmljZWFjY291bnQiOnsibmFtZSI6ImRlZmF1bHQiLCJ1aWQiOiJlODg5ZjJiNC1jZjlmLTQzZTMtOGI1NC1kM2U1MTljZjcyMmYifX0sIm5iZiI6MTczMzAxOTI3MSwic3ViIjoic3lzdGVtOnNlcnZpY2VhY2NvdW50OmRlZmF1bHQ6ZGVmYXVsdCJ9.A61WEydxYIXdM3hqV8m0dqf-LT_YUFRR4UZYKbOxidvvmvElUWtMt5z_1uk2mKXEgf8UzqlrGdaCrYbYXe-WUpJRhNPxsSXlXhYZMPu14Nysa8yF9GrjDvbBTY0CELVcY_T6PI6bONlh2R7Ti5UE2WIapyHJUu66AgMjOH2e3qzLumUtUkoWXF8wvDU1QV4fNpadQy6HBrcYxDU2c3Ge47bkp-k69QWLhrGTvBzow-7GTuj3NS8eFgJpMA3PnY28eKsXyzgKsMLkdA9ePQpOEiMs-nNmQuAnwj3HIiTa0RBF3qBMxqyE9o4y8fGf7K6a68HAAxZmAZ4eHJcxaWl0ZA"; // 替换为您的Token
////        ApiKeyAuth auth = (ApiKeyAuth) client.getAuthentication("BearerToken");
//
//        client.setVerifyingSsl(false);
//        Configuration.setDefaultApiClient(client);
//        CoreV1Api api = new CoreV1Api(client);
//
//        String podIp = "10.100.2.69"; // 替换为你的 Pod IP
//        String namespace = "kube-test"; // 替换为你的命名空间
//
//        // 获取 Pod 列表
//        V1PodList podList = api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null, null);
//        for (V1Pod pod : podList.getItems()) {
//            // 检查 Pod 的 IP 是否匹配
//            if (pod.getStatus().getPodIP().equals(podIp)) {
//                String nodeName = pod.getSpec().getNodeName();
//                System.out.println("Pod " + podIp + " is running on node " + nodeName);
//
//                // 获取节点信息
//                V1Node node = api.readNode(nodeName, null);
//                System.out.println("Node details: " + node);
//                break;
//            }
//        }
    }

    public static String getServiceName(String str) {
        int lastIndex = str.lastIndexOf('-');
        if (lastIndex == -1) {
            // 如果没有下划线，返回原字符串
            return str;
        }
        int secondLastIndex = str.lastIndexOf('-', lastIndex - 1);
        if (secondLastIndex == -1) {
            // 如果只有一个下划线，返回到该下划线前的部分
            return str.substring(0, lastIndex);
        }
        return str.substring(0, secondLastIndex);
    }
}