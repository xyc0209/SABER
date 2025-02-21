package com.refactor.executor.agent;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * @description:
 * @author: xyc
 * @date: 2024-10-10 10:39
 */
@Component
public class IngressAgent {

        public static void main(String[] args) throws IOException, ApiException {

            try {
                ApiClient client = Config.defaultClient();
                client.setBasePath("https://10.245.1.233:6443");
                client.setApiKey("Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6Ik9oRUlMOXR3OHdFcjduUEx1VjFLTE94WlQ4Tkd2ZnRsX2NoM0REMEMzUWsifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJhcGlpbnZva2VyLXRva2VuIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6ImFwaWludm9rZXIiLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiIwNzE1MjIwMi01OGNkLTQ4YTEtODk2Mi03YTk3YzkzMDg2NjkiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6a3ViZS1zeXN0ZW06YXBpaW52b2tlciJ9.LbjNXWHo76Y_A4JPU0qbTdjUlZ9tZDL76cXyE9Ap8ZSN6YHRzMLHReHaKnAI38uIWhlfCWITNTn6iLqtv8iMmeSpjbwi2FFfXo8MLmN_RmrAb529dXPrS8RPmdwlvZ2Y8MMqLW1VvvDH1mEF-uPxoeFi-G693G9cW3u_kwkTEnFBTBgab5-xPkAx0v9IJc8vYX31guUpdZ4bNpb3ijHglv4OM0H8OHuYlyBELOnGUpziIB39Vw_uDUblbjlSodRrnEGR7TC2ECqsXzc9d5pOtPS2o9hoj0LY12hjmKqAjhUrBLyhe1E2yKT65SG5zpsWmHgQPYoNqqOqdADVn3_UoQ");
                client.setVerifyingSsl(false);

                Configuration.setDefaultApiClient(client);
                // 将配置设置为默认的 API 客户端
                io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
                NetworkingV1Api api = new NetworkingV1Api(client);

                V1IngressList ingressList = api.listNamespacedIngress("kube-test").execute();

                V1Ingress ingress = new V1Ingress();
                ingress.metadata(new V1ObjectMeta().name("test-ingress").namespace("kube-test"));
                ingress.spec(new V1IngressSpec().ingressClassName("nginx").rules(Collections.singletonList(
                        new V1IngressRule().http(new V1HTTPIngressRuleValue().paths(Collections.singletonList(
                                new V1HTTPIngressPath().path("/api/orders").backend(new V1IngressBackend()
                                        .service(new V1IngressServiceBackend().name("service-split")
                                                .port(new V1ServiceBackendPort().number(8089))))))))));

                if (ingressList.getItems().isEmpty()) {
                    api.createNamespacedIngress("kube-test", ingress).execute();
                    System.out.println("Created a new Ingress resource");
                } else {
                    V1Ingress existingIngress = ingressList.getItems().get(0);
//                    existingIngress.spec(ingress.getSpec());
                    for (V1IngressRule rule : existingIngress.getSpec().getRules()) {
                        System.out.println("rule.getHttp()"+rule.getHttp());
                        if (rule.getHttp() != null) {
                            for (V1HTTPIngressPath path : rule.getHttp().getPaths()) {
                                // 修改特定路径对应的服务名
                                if (path.getBackend().getService().getName().equals("test-service")) {
                                    System.out.println("============");
                                    path.getBackend().setService(new V1IngressServiceBackend()
                                            .name("newservice")
                                            .port(new V1ServiceBackendPort().number(8099)));
                                    System.out.println("Updated Ingress path to new service: ");
                                }
                            }
                        }
                    }
                    System.out.println(existingIngress.toString());
                    V1Ingress updatedIngress = api.replaceNamespacedIngress(existingIngress.getMetadata().getName(), "kube-test", existingIngress).execute();
                    System.out.println("Updated the existing Ingress resource" + updatedIngress.toString());
                }
            } catch (ApiException e) {
                System.err.println("Exception when calling NetworkingV1Api#listNamespacedIngress");
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public  String operateIngressByBatch(ApiClient apiClient, Map<String, Map<String, String>> serviceDetails){
            updateIngress(apiClient,serviceDetails);

            return "INGRESS UPDATE SUCCESS";

        }

    public static String updateIngress(ApiClient client, Map<String, Map<String, String>> serviceDetails) {
        try {

            NetworkingV1Api api = new NetworkingV1Api(client);

            // 列出命名空间中的 Ingress
            NetworkingV1Api.APIlistNamespacedIngressRequest ingressRequest = api.listNamespacedIngress("kube-test");
            V1IngressList ingressList = ingressRequest.execute();

            V1Ingress existingIngress = null;
            boolean updated = false;

            // 检查现有的 Ingress 资源
            if (ingressList.getItems() != null && !ingressList.getItems().isEmpty()) {
                for (V1Ingress ingress : ingressList.getItems()) {
                    existingIngress = ingress;
                    System.out.println("Ingress Name: " + ingress.getMetadata().getName());
                    System.out.println("Namespace: " + ingress.getMetadata().getNamespace());

                    // 遍历每个服务的详细信息
                    for (Map<String, String> detail : serviceDetails.values()) {
                        if (!detail.containsKey("rulePath"))
                            continue;
                        String rulePath = detail.get("rulePath");
                        String serviceName = detail.get("serviceName");
                        Integer servicePort = Integer.valueOf(detail.get("servicePort"));

                        boolean ruleFound = false;

                        // 遍历每个规则
                        for (V1IngressRule rule : ingress.getSpec().getRules()) {
                            if (rule.getHttp() != null) {
                                for (V1HTTPIngressPath path : rule.getHttp().getPaths()) {
                                    // 检查服务名是否匹配
                                    if (path.getBackend().getService().getName().equals(serviceName)) {
                                        System.out.println("path.getBackend().getService().getName()" +path.getBackend().getService().getName());
                                        System.out.println("serviceName" +serviceName);
                                        // 更新路径和端口
                                        path.setPath(rulePath);
                                        path.getBackend().getService().setPort(new V1ServiceBackendPort().number(servicePort));
                                        updated = true;  // 标记为已更新
                                        ruleFound = true; // 标记规则已找到
                                        System.out.println("Updated Ingress rule for service: " + serviceName);
                                    }
                                }
                            }
                        }

                        // 如果没有找到规则，则添加新的规则
                        if (!ruleFound) {
                            V1HTTPIngressPath newPath = new V1HTTPIngressPath()
                                    .path(rulePath)
                                    .pathType("Prefix")
                                    .backend(new V1IngressBackend()
                                            .service(new V1IngressServiceBackend()
                                                    .name(serviceName)
                                                    .port(new V1ServiceBackendPort().number(servicePort))));

                            V1IngressRule newRule = new V1IngressRule()
                                    .http(new V1HTTPIngressRuleValue().paths(Collections.singletonList(newPath)));

                            existingIngress.getSpec().getRules().add(newRule);
                            updated = true; // 标记为已更新
                            System.out.println("Added new Ingress rule for service: " + serviceName);
                        }
                    }
                    // 一旦更新了规则，就统一更新 Ingress
                    if (updated) {
                        api.replaceNamespacedIngress(existingIngress.getMetadata().getName(), "kube-test", existingIngress).execute();
                        System.out.println("Saved updated Ingress resource for service: " + existingIngress.getMetadata().getName());
                    }
                }
            }

            // 如果没有现有 Ingress，则创建新的 Ingress
            if (existingIngress == null) {
                // 创建新的 Ingress
                V1Ingress newIngress = new V1Ingress()
                        .metadata(new V1ObjectMeta().name("multi-service-ingress").namespace("kube-test"))
                        .spec(new V1IngressSpec().ingressClassName("nginx"));

                // 添加所有规则
                List<V1IngressRule> rules = new ArrayList<>();

                for (Map<String, String> detail : serviceDetails.values()) {
                    String rulePath = detail.get("rulePath");
                    String serviceName = detail.get("serviceName");
                    Integer servicePort = Integer.valueOf(detail.get("servicePort"));

                    V1HTTPIngressPath newPath = new V1HTTPIngressPath()
                            .path(rulePath)
                            .pathType("Prefix")
                            .backend(new V1IngressBackend()
                                    .service(new V1IngressServiceBackend()
                                            .name(serviceName)
                                            .port(new V1ServiceBackendPort().number(servicePort))));

                    V1IngressRule newRule = new V1IngressRule()
                            .http(new V1HTTPIngressRuleValue().paths(Collections.singletonList(newPath)));

                    rules.add(newRule);
                }

                newIngress.setSpec(new V1IngressSpec().ingressClassName("nginx").rules(rules));
                api.createNamespacedIngress("kube-test", newIngress).execute();
                System.out.println("Created a new Ingress resource with all rules.");
            }
        } catch (ApiException e) {
            System.err.println("Exception when calling NetworkingV1Api#listNamespacedIngress");
            System.err.println("创建ingress异常:" + e.getResponseBody());
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Ingress update process completed.";
    }
//    public static String updateIngress(String rulePath, String serviceName, Integer port) {
//        try {
//            ApiClient client = Config.defaultClient();
//            NetworkingV1Api api = new NetworkingV1Api(client);
//
//            // 列出命名空间中的 Ingress
//            NetworkingV1Api.APIlistNamespacedIngressRequest ingressRequest = api.listNamespacedIngress("kube-test");
//            V1IngressList ingressList = ingressRequest.execute();
//
//            boolean updated = false;
//            V1Ingress existingIngress = null;
//
//            // 检查现有的 Ingress 资源
//            if (ingressList.getItems() != null && !ingressList.getItems().isEmpty()) {
//                for (V1Ingress ingress : ingressList.getItems()) {
//                    existingIngress = ingress;
//                    System.out.println("Ingress Name: " + ingress.getMetadata().getName());
//                    System.out.println("Namespace: " + ingress.getMetadata().getNamespace());
//
//                    // 遍历每个规则
//                    for (V1IngressRule rule : ingress.getSpec().getRules()) {
//                        if (rule.getHttp() != null) {
//                            for (V1HTTPIngressPath path : rule.getHttp().getPaths()) {
//                                // 检查服务名是否匹配
//                                if (path.getBackend().getService().getName().equals(serviceName)) {
//                                    // 更新路径和端口
//                                    path.setPath(rulePath);
//                                    path.getBackend().getService().setPort(new V1ServiceBackendPort().number(port));
//                                    updated = true;
//                                    System.out.println("Updated Ingress rule for service: " + serviceName);
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            // 如果没有找到更新的规则，则添加新的规则
//            if (!updated) {
//                if (existingIngress != null) {
//                    // 添加新的规则到现有的 Ingress
//                    V1HTTPIngressPath newPath = new V1HTTPIngressPath()
//                            .path(rulePath)
//                            .pathType("Prefix")
//                            .backend(new V1IngressBackend()
//                                    .service(new V1IngressServiceBackend()
//                                            .name(serviceName)
//                                            .port(new V1ServiceBackendPort().number(port))));
//
//                    V1IngressRule newRule = new V1IngressRule()
//                            .http(new V1HTTPIngressRuleValue().paths(Collections.singletonList(newPath)));
//
//                    existingIngress.getSpec().getRules().add(newRule);
//                    api.replaceNamespacedIngress(existingIngress.getMetadata().getName(), "kube-test", existingIngress);
//                    System.out.println("Added new Ingress rule for service: " + serviceName);
//                } else {
//                    // 如果没有 Ingress，则创建新的 Ingress
//                    V1Ingress newIngress = new V1Ingress()
//                            .metadata(new V1ObjectMeta().name(serviceName + "-ingress").namespace("kube-test"))
//                            .spec(new V1IngressSpec().ingressClassName("nginx").rules(Collections.singletonList(
//                                    new V1IngressRule().http(new V1HTTPIngressRuleValue().paths(Collections.singletonList(
//                                            new V1HTTPIngressPath().path(rulePath).pathType("Prefix").backend(new V1IngressBackend()
//                                                    .service(new V1IngressServiceBackend().name(serviceName)
//                                                            .port(new V1ServiceBackendPort().number(port))))))))));
//
//                    api.createNamespacedIngress("kube-test", newIngress);
//                    System.out.println("Created a new Ingress resource for service: " + serviceName);
//                }
//            } else {
//                System.out.println("Updated existing Ingress resource");
//            }
//        } catch (ApiException e) {
//            System.err.println("Exception when calling NetworkingV1Api#listNamespacedIngress");
//            System.err.println("创建ingress异常:" + e.getResponseBody());
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return "Ingress update process completed.";
//    }
//        public static String updateIngress(String rulePath, String serviceName,Integer port){
//            try {
//                ApiClient client = Config.defaultClient();
//                NetworkingV1Api api = new NetworkingV1Api(client);
//
//                NetworkingV1Api.APIlistNamespacedIngressRequest ingressRequest = api.listNamespacedIngress("kube-test");
//                V1IngressList ingressList = ingressRequest.execute();
//                if (ingressList.getItems() == null || ingressList.getItems().isEmpty()){
//                    System.out.println("No Ingress resources found in the 'kube-test' namespace.");
//                    return "No Ingress resources found.";
//                }
//                for (V1Ingress ingress : ingressList.getItems()) {
//                    System.out.println("Ingress Name: " + ingress.getMetadata().getName());
//                    System.out.println("Namespace: " + ingress.getMetadata().getNamespace());
//                    System.out.println("Annotations: " + ingress.getMetadata().getAnnotations());
//                }
//                V1Ingress ingress = new V1Ingress();
//                ingress.metadata(new V1ObjectMeta().name(serviceName + "-ingress").namespace("kube-test"));
//                ingress.spec(new V1IngressSpec().ingressClassName("nginx").rules(Collections.singletonList(
//                        new V1IngressRule().http(new V1HTTPIngressRuleValue().paths(Collections.singletonList(
//                                new V1HTTPIngressPath().path(rulePath).pathType("Prefix").backend(new V1IngressBackend()
//                                        .service(new V1IngressServiceBackend().name(serviceName)
//                                                .port(new V1ServiceBackendPort().number(port))))))))));
//                api.createNamespacedIngress("kube-test", ingress);
//                System.out.println("Created a new Ingress resource");
//            } catch (ApiException e) {
//                System.err.println("Exception when calling NetworkingV1Api#listNamespacedIngress");
//                System.err.println("创建ingress异常:" + e.getResponseBody());
//                e.printStackTrace();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return "Ingresss Update";
//        }

    public void testIngress(K8sClusterAgent k8sClusterAgent) throws IOException, ApiException {
        ApiClient client = k8sClusterAgent.getClient();
        System.out.println(client.getBasePath());
        NetworkingV1Api api = new NetworkingV1Api(client);

        // 列出命名空间中的 Ingress
        NetworkingV1Api.APIlistNamespacedIngressRequest ingressRequest = api.listNamespacedIngress("kube-test");
        V1IngressList ingressList = ingressRequest.execute();

        V1Ingress existingIngress = null;
        boolean updated = false;

        // 检查现有的 Ingress 资源
        if (ingressList.getItems() != null && !ingressList.getItems().isEmpty()) {
            for (V1Ingress ingress : ingressList.getItems()) {
                existingIngress = ingress;
                System.out.println("Ingress Name: " + ingress.getMetadata().getName());
                System.out.println("Namespace: " + ingress.getMetadata().getNamespace());
            }
        }
    }
        public String deleteIngress() throws IOException, ApiException {
            ApiClient client = Config.defaultClient();
            NetworkingV1Api api = new NetworkingV1Api(client);

            // 指定要检查的服务名
            String targetServiceName = "your-service-name"; // 替换为要检查的服务名称
            String namespace = "kube-test"; // 指定命名空间

            // 获取指定命名空间的所有 Ingress
            NetworkingV1Api.APIlistNamespacedIngressRequest ingressRequest = api.listNamespacedIngress("kube-test");
            V1IngressList ingressList = ingressRequest.execute();

            // 遍历 Ingress 列表
            for (V1Ingress ingress : ingressList.getItems()) {
                // 获取 Ingress 的 metadata
                V1ObjectMeta metadata = ingress.getMetadata();
                String ingressName = metadata.getName();

                // 获取并遍历 Ingress 规则
                List<V1IngressRule> rules = ingress.getSpec().getRules();
                if (rules != null) {
                    // 使用迭代器遍历规则以便可以安全地删除
                    Iterator<V1IngressRule> ruleIterator = rules.iterator();
                    while (ruleIterator.hasNext()) {
                        V1IngressRule rule = ruleIterator.next();

                        // 检查每个规则的 backend
                        if (rule.getHttp() != null && rule.getHttp().getPaths() != null) {
                            boolean ruleMatches = rule.getHttp().getPaths().stream()
                                    .anyMatch(path -> {
                                        V1IngressServiceBackend backend = path.getBackend().getService();
                                        return backend.getName().equals(targetServiceName);
                                    });

                            // 如果规则后端匹配指定服务名，删除该规则
                            if (ruleMatches) {
                                ruleIterator.remove(); // 删除规则
                            }
                        }
                    }
                }

                // 更新 Ingress，如果规则发生了变化，则进行更新
                api.replaceNamespacedIngress(ingressName, namespace, ingress).execute();
            }
            return "delete success";

        }
    }
