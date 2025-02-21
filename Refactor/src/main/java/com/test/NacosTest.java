package com.test;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;

import java.util.*;


/**
 * @description:
 * @author: xyc
 * @date: 2024-11-30 18:21
 */
public class NacosTest {
    public static void main(String[] args) throws NacosException {
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
                System.out.println("Instance: " + instance);
                Map<String, String> metadata = new HashMap<>();
                metadata.put("node", "node1");
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