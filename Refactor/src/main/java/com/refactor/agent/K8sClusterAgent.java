package com.refactor.agent;

import com.google.gson.Gson;
import io.kubernetes.client.common.KubernetesType;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @description:
 * @author: xyc
 * @date: 2024-09-28 16:12
 */
public class K8sClusterAgent {

    public static String createDeploymentFromImage(String imageName, String serviceName, Integer port) {
        try {
            // 初始化 Kubernetes API 客户端
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);

            // 创建 Deployment 资源
            V1Deployment deployment = new V1Deployment()
                    .metadata(new V1ObjectMeta()
                            .name(serviceName)
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
                                                            .imagePullPolicy("IfNotPresent")
                                                            .ports(Arrays.asList(
                                                                    new V1ContainerPort()
                                                                            .containerPort(port))))))));
            // 创建 Service 资源
            V1Service service = new V1Service()
                    .metadata(new V1ObjectMeta()
                            .name(serviceName)
                            .namespace("kube-com.test")
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
            Gson gson = new Gson();
            String yamlContent = gson.toJson(deployment);
            String yamlFile = serviceName + "_deployment.yaml";
            writeToYamlFileAll(yamlFile, deployment, service);
            System.out.println(gson.toJson(deployment));
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
            writer.write(Yaml.dump(deployment));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeToYamlFileAll(String fileName, V1Deployment deployment, V1Service service) {
        try (FileWriter writer = new FileWriter(fileName)) {
            Yaml yaml = new Yaml();

            List<KubernetesType> objects = new ArrayList<>();
            objects.add(deployment);
            objects.add(service);
            yaml.dumpAll(objects.iterator(), writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        createDeploymentFromImage("172.16.17.46:8085/testreposity/testserviceimage:split_1", "ts-verification-code-service", 8089);
    }
}