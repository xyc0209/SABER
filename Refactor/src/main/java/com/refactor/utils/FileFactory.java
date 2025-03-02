package com.refactor.utils;


import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.refactor.Adaptive.RMonitor;
import com.refactor.chain.analyzer.layer.LayerType;
import com.refactor.chain.utils.Node;
import com.refactor.context.ServiceContext;
import com.refactor.enumeration.TemplateFile;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class FileFactory {

    private static final String CONFIG_FOLDER_NAME = "config";
    private static final String AUTO_CONFIG_FILE_NAME = "CSLoadBalancer.java";
    private static final String AUTO_CONFIG = "CSLoadBalancerConfig.java";

    private static final String POD_CLASS_NAME = "PodInfo.java";
    private static final String CONTAINER_CLASS_NAME = "ContainersItem.java";
    private static final String USAGE_CLASS_NAME = "Usage.java";

    private static final Yaml yamlReader = new Yaml();

    public  static List<String> getPomFiles(String servicesDirectory) throws IOException {
        Path start= Paths.get(servicesDirectory);
        List<String> pomFiles = new ArrayList<>();
        int maxDepth = 10;
        Stream<Path> stream = Files.find(start,maxDepth,(filepath, attributes) -> String.valueOf(filepath).contains("pom.xml"));

        pomFiles = stream.sorted().map(String::valueOf).filter(filepath ->{
            if(String.valueOf(filepath).toLowerCase().contains(".mvn") || String.valueOf(filepath).toLowerCase().contains("gradle")){
                return false;
            }
            else {
                return true;
            }
        }).collect(Collectors.toList());
        return  pomFiles;
    }

//    private static String getAutoConfigurationContent(String relativePath) {
//        relativePath = relativePath.replace(File.separator, ".");
//
//        return "package " + relativePath + ";\n\n" +
//                "import com.mbs.mclient.aspect.LoggingAspect;\n" +
//                "import org.springframework.context.annotation.Bean;\n" +
//                "import org.springframework.context.annotation.Configuration;\n" +
//                "import org.springframework.context.annotation.EnableAspectJAutoProxy;\n\n" +
//                "@Configuration\n" +
//                "@EnableAspectJAutoProxy\n" +
//                "public class LoggableAutoConfiguration {\n\n" +
//                "    @Bean\n" +
//                "    public LoggingAspect loggingAspect() {\n" +
//                "        return new LoggingAspect();\n" +
//                "    }\n" +
//                "}";
//    }
    private static String getCSLoadBalancerConfigContent(String relativePath, Set<String> serviceSet, String service) {
        relativePath = relativePath.replace(File.separator, ".");

        // 构建 @LoadBalancerClient 注解部分
        StringBuilder loadBalancerClients = new StringBuilder();
        for (String serviceName : serviceSet) {
            if (!serviceName.equals(service)){
                loadBalancerClients.append("@LoadBalancerClient(name = \"")
                        .append(serviceName)
                        .append("\", configuration = CSLoadBalancerConfig.class)\n");
            }
        }

        return "package " + relativePath + ";\n\n" +
                "import org.springframework.beans.factory.ObjectProvider;\n" +
                "import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;\n" +
                "import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;\n" +
                "import org.springframework.context.annotation.Bean;\n" +
                "import org.springframework.context.annotation.Configuration;\n\n" +
                loadBalancerClients.toString() + // 添加生成的 LoadBalancerClient 注解
                "@Configuration\n" +
                "public class CSLoadBalancerConfig {\n\n" +
                "    @Bean\n" +
                "    public CSLoadBalancer myLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> supplier) {\n" +
                "        return new CSLoadBalancer(supplier);\n" +
                "    }\n" +
                "}\n";
    }

    private static String getUsageContent(String relativePath) {
        relativePath = relativePath.replace(File.separator, ".");
        return "package " + relativePath + ";\n\n" +
                "import com.google.gson.annotations.SerializedName;\n" +
                "import javax.annotation.Generated;\n\n" +
                "@Generated(\"com.robohorse.robopojogenerator\")\n" +
                "public class Usage {\n\n" +
                "    @SerializedName(\"memory\")\n" +
                "    private String memory;\n\n" +
                "    @SerializedName(\"cpu\")\n" +
                "    private String cpu;\n\n" +
                "    public void setMemory(String memory) {\n" +
                "        this.memory = memory;\n" +
                "    }\n\n" +
                "    public String getMemory() {\n" +
                "        return memory;\n" +
                "    }\n\n" +
                "    public void setCpu(String cpu) {\n" +
                "        this.cpu = cpu;\n" +
                "    }\n\n" +
                "    public String getCpu() {\n" +
                "        return cpu;\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public String toString() {\n" +
                "        return\n" +
                "            \"Usage{\" + \n" +
                "            \"memory = '\" + memory + '\\'' + \n" +
                "            \", cpu = '\" + cpu + '\\'' + \n" +
                "            \"}\";\n" +
                "    }\n" +
                "}\n";
    }

    // 返回 CSLoadBalancer 类的内容
    private static String getCSLoadBalancerContent(String relativePath) {
        relativePath = relativePath.replace(File.separator, ".");
        return "package " + relativePath + ";\n\n" +
                "import com.google.gson.Gson;\n" +
                "import io.kubernetes.client.openapi.ApiException;\n" +
                "import io.kubernetes.client.openapi.apis.CustomObjectsApi;\n" +
                "import org.springframework.beans.factory.ObjectProvider;\n" +
                "import org.springframework.beans.factory.annotation.Autowired;\n" +
                "import org.springframework.beans.factory.annotation.Value;\n" +
                "import org.springframework.cloud.client.ServiceInstance;\n" +
                "import org.springframework.cloud.client.loadbalancer.reactive.DefaultResponse;\n" +
                "import org.springframework.cloud.client.loadbalancer.reactive.Request;\n" +
                "import org.springframework.cloud.client.loadbalancer.reactive.Response;\n" +
                "import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;\n" +
                "import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;\n" +
                "import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;\n" +
                "import org.springframework.core.ParameterizedTypeReference;\n" +
                "import org.springframework.http.HttpEntity;\n" +
                "import org.springframework.http.HttpMethod;\n" +
                "import org.springframework.http.ResponseEntity;\n" +
                "import org.springframework.web.client.RestTemplate;\n" +
                "import reactor.core.publisher.Mono;\n" +
                "import java.math.BigDecimal;\n" +
                "import java.net.InetAddress;\n" +
                "import java.net.UnknownHostException;\n" +
                "import java.util.HashMap;\n" +
                "import java.util.List;\n" +
                "import java.util.Map;\n" +
                "import java.util.concurrent.atomic.AtomicReference;\n\n" +
                "public class CSLoadBalancer implements ReactorServiceInstanceLoadBalancer {\n\n" +
                "    @Value(\"${spring.application.name}\")\n" +
                "    private String serviceName;\n\n" +
                "    @Autowired\n" +
                "    ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;\n\n" +
                "    @Autowired\n" +
                "    private RestTemplate restTemplate;\n\n" +
                "    // 缓存Pod信息及时间戳\n" +
                "    private final AtomicReference<PodCache> podCache = new AtomicReference<>(new PodCache());\n\n" +
                "    // 缓存数据结构，保存Pod信息和缓存时间戳\n" +
                "    static class PodCache {\n" +
                "        Map<String, List<PodInfo>> podInfos;\n" +
                "        long timestamp;\n\n" +
                "        PodCache() {\n" +
                "            this.timestamp = 0;\n" +
                "        }\n\n" +
                "        PodCache(Map<String, List<PodInfo>> podInfos, long timestamp) {\n" +
                "            this.podInfos = podInfos;\n" +
                "            this.timestamp = timestamp;\n" +
                "        }\n" +
                "    }\n\n" +
                "    public CSLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {\n" +
                "        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;\n" +
                "    }\n\n" +
                "    @SuppressWarnings(\"rawtypes\")\n" +
                "    @Override\n" +
                "    public Mono<Response<ServiceInstance>> choose(Request request) {\n" +
                "        System.out.println(\"CHOOSE TEST\");\n" +
                "        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider\n" +
                "                .getIfAvailable(NoopServiceInstanceListSupplier::new);\n" +
                "        return supplier.get().next()\n" +
                "                .map(serviceInstances -> {\n" +
                "                    System.out.println(\"INSTANCES\" + serviceInstances.toString());\n" +
                "                    return processInstanceResponse(supplier, serviceInstances);\n" +
                "                });\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public Mono<Response<ServiceInstance>> choose() {\n" +
                "        System.out.println(\"CHOOSE TEST NULL\");\n" +
                "        return ReactorServiceInstanceLoadBalancer.super.choose();\n" +
                "    }\n\n" +
                "    private Response<ServiceInstance> processInstanceResponse(ServiceInstanceListSupplier supplier,\n" +
                "                                                              List<ServiceInstance> serviceInstances) {\n" +
                "        Response<ServiceInstance> serviceInstanceResponse = getInstanceResponse(serviceInstances);\n" +
                "        // 直接返回选择的服务实例响应\n" +
                "        return serviceInstanceResponse;\n" +
                "    }\n\n" +
                "    // 获取本地节点名称\n" +
                "    private String getLocalNodeName(Map<String, List<PodInfo>> podInfos) {\n" +
                "        try {\n" +
                "            String ip = InetAddress.getLocalHost().getHostAddress();\n" +
                "            for (PodInfo podInfo : podInfos.get(serviceName)) {\n" +
                "                for (ContainersItem containersItem : podInfo.getContainers()) {\n" +
                "                    // 处理逻辑\n" +
                "                }\n" +
                "            }\n" +
                "            for (PodInfo podInfo : podInfos.get(serviceName)) {\n" +
                "                if (podInfo.getPodIp().equals(ip)) {\n" +
                "                    return podInfo.getNodeName();\n" +
                "                }\n" +
                "            }\n" +
                "        } catch (UnknownHostException e) {\n" +
                "            e.printStackTrace();\n" +
                "        }\n" +
                "        return \"default-node\"; // 默认节点\n" +
                "    }\n\n" +
                "    public static Map<String, BigDecimal> getAllResource(Map<String, List<PodInfo>> servicePodDetails, String service) {\n" +
                "        Map<String, BigDecimal> resourceMap = new HashMap<>();\n" +
                "        List<PodInfo> podInfoList = servicePodDetails.get(service);\n" +
                "        BigDecimal cpuUsed = new BigDecimal(0);\n" +
                "        BigDecimal ramUsed = new BigDecimal(0);\n" +
                "        for (PodInfo podInfo : podInfoList) {\n" +
                "            for (ContainersItem containersItem : podInfo.getContainers()) {\n" +
                "                String cpu = containersItem.getUsage().getCpu();\n" +
                "                String ram = containersItem.getUsage().getMemory();\n" +
                "                cpuUsed = cpuUsed.add(new BigDecimal(cpu.substring(0, cpu.length() - 1)));\n" +
                "                ramUsed = ramUsed.add(new BigDecimal(ram.substring(0, ram.length() - 2)));\n" +
                "            }\n" +
                "        }\n" +
                "        resourceMap.put(\"cpu\", cpuUsed);\n" +
                "        resourceMap.put(\"ram\", ramUsed);\n" +
                "        return resourceMap;\n" +
                "    }\n\n" +
                "    // 刷新缓存，调用Kubernetes API获取最新的Pod信息\n" +
                "    private void refreshPodCache() {\n" +
                "        String url = \"http://cluster-agent/api/v1/clusteragent/podinfo\";\n" +
                "        ResponseEntity<Map<String, List<PodInfo>>> response = restTemplate.exchange(\n" +
                "                url,\n" +
                "                HttpMethod.GET,\n" +
                "                HttpEntity.EMPTY, // 无需请求体\n" +
                "                new ParameterizedTypeReference<Map<String, List<PodInfo>>>() {}\n" +
                "        );\n" +
                "        Map<String, List<PodInfo>> podInfos = response.getBody();\n" +
                "        podCache.set(new PodCache(podInfos, System.currentTimeMillis()));\n" +
                "    }\n\n" +
                "    // 负载均衡算法\n" +
                "    private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances) {\n" +
                "        long currentTime = System.currentTimeMillis();\n" +
                "        PodCache cache = podCache.get();\n\n" +
                "        // 每隔1分钟刷新缓存\n" +
                "        if (currentTime - cache.timestamp > 60 * 1000) {\n" +
                "            // 重新调用Kubernetes API更新缓存\n" +
                "            refreshPodCache();\n" +
                "            cache = podCache.get();\n" +
                "        }\n\n" +
                "        String nodeName = getLocalNodeName(cache.podInfos);\n" +
                "        // 优先选择在同一节点的实例\n" +
                "        BigDecimal cpuUsage = getAllResource(cache.podInfos, serviceName).get(\"cpu\");\n" +
                "        BigDecimal ramUsage = getAllResource(cache.podInfos, serviceName).get(\"ram\");\n" +
                "        for (ServiceInstance instance : instances) {\n" +
                "            if (instance.getMetadata().get(\"node\").equals(nodeName) && cpuUsage.compareTo(new BigDecimal(2000000000)) < 0 && ramUsage.compareTo(new BigDecimal(4194304)) < 0) {\n" +
                "                return new DefaultResponse(instance);\n" +
                "            }\n" +
                "        }\n\n" +
                "        // 如果没有找到符合条件的实例，返回第一个实例\n" +
                "        return instances.isEmpty() ? null : new DefaultResponse(instances.get(0));\n" +
                "    }\n\n" +
                "    public static void main(String[] args) {\n" +
                "        long currentTime = System.currentTimeMillis();\n" +
                "        System.out.println(\"currentTime\" + currentTime);\n" +
                "    }\n" +
                "}\n";
    }

    private static String getPodInfoContent(String relativePath) {
        relativePath = relativePath.replace(File.separator, ".");
        return "package " + relativePath + ";\n\n" +
                "import lombok.Data;\n" +
                "import lombok.NoArgsConstructor;\n" +
                "import java.util.List;\n\n" +
                "@Data\n" +
                "@NoArgsConstructor\n" +
                "public class PodInfo {\n" +
                "    private String name;\n" +
                "    private String podIp;\n" +
                "    private String nameSpace;\n\n" +
                "    private List<ContainersItem> containers;\n" +
                "    private String nodeName;\n" +
                "}\n";
    }

    private static String getContainersItemContent(String relativePath) {
        relativePath = relativePath.replace(File.separator, ".");
        return "package " + relativePath + ";\n\n" +
                "import com.google.gson.annotations.SerializedName;\n" +
                "import javax.annotation.Generated;\n\n" +
                "@Generated(\"com.robohorse.robopojogenerator\")\n" +
                "public class ContainersItem {\n\n" +
                "    @SerializedName(\"usage\")\n" +
                "    private Usage usage;\n\n" +
                "    @SerializedName(\"name\")\n" +
                "    private String name;\n\n" +
                "    public void setUsage(Usage usage) {\n" +
                "        this.usage = usage;\n" +
                "    }\n\n" +
                "    public Usage getUsage() {\n" +
                "        return usage;\n" +
                "    }\n\n" +
                "    public void setName(String name) {\n" +
                "        this.name = name;\n" +
                "    }\n\n" +
                "    public String getName() {\n" +
                "        return name;\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public String toString() {\n" +
                "        return\n" +
                "            \"ContainersItem{\" + \n" +
                "            \"usage = '\" + usage + '\\'' + \n" +
                "            \", name = '\" + name + '\\'' + \n" +
                "            \"}\";\n" +
                "    }\n" +
                "}\n";
    }
    public static void addConfig(List<String> applicationPath, Set<String> serviceSet, String service) throws FileNotFoundException {
        File applicationFile = new File(applicationPath.get(0));
        Charset charset = Charset.forName("UTF-8");
        CompilationUnit cu = StaticJavaParser.parse(applicationFile, charset);
        System.out.println(cu.getClass());
//        List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
        String startupClassPath = applicationFile.getParent();
        File configFolder = new File(startupClassPath + File.separator + CONFIG_FOLDER_NAME);
        if (!configFolder.exists()) {
           configFolder.mkdirs();
        }
        String cofigPath = startupClassPath + File.separator + CONFIG_FOLDER_NAME;
        String loadBalancerFilePath = cofigPath + File.separator + AUTO_CONFIG_FILE_NAME;
        String configurationPath = cofigPath + File.separator + AUTO_CONFIG;
        String usagePath =  cofigPath + File.separator + USAGE_CLASS_NAME;
        String podPath =  cofigPath + File.separator + POD_CLASS_NAME;
        String containerPath =  cofigPath + File.separator + CONTAINER_CLASS_NAME;
        int javaIndex = loadBalancerFilePath.indexOf("java" + File.separator);
        if (javaIndex != -1) {
            String relativePath = cofigPath.substring(javaIndex + 5);

            File autoConfigFile = new File(loadBalancerFilePath);
            if (!autoConfigFile.exists()) {
                // 创建LoggableAutoConfiguration.java文件
                try {
                    FileWriter writer = new FileWriter(autoConfigFile);
                    writer.write(getCSLoadBalancerContent(relativePath));
                    writer.close();
                    System.out.println("CustomLoadBalancer.java文件已创建");
                } catch (IOException e) {
                    System.out.println("无法创建CustomLoadBalancer.java文件");
                    e.printStackTrace();
                }
            } else {
                System.out.println("CustomLoadBalancer.java文件已存在");
            }
            File configFile = new File(configurationPath);
            if (!configFile.exists()) {
                // 创建LoggableAutoConfiguration.java文件
                try {
                    FileWriter writer = new FileWriter(configFile);
                    writer.write(getCSLoadBalancerConfigContent(relativePath, serviceSet, service));
                    writer.close();
                    System.out.println("CSLoadBalancerConfig.java文件已创建");
                } catch (IOException e) {
                    System.out.println("无法创建CSLoadBalancerConfig.java文件");
                    e.printStackTrace();
                }
            } else {
                System.out.println("CSLoadBalancerConfig.java文件已存在");
            }

            File usageFile = new File(usagePath);
            if (!usageFile.exists()) {
                // 创建LoggableAutoConfiguration.java文件
                try {
                    FileWriter writer = new FileWriter(usageFile);
                    writer.write(getUsageContent(relativePath));
                    writer.close();
                    System.out.println("Usage.java文件已创建");
                } catch (IOException e) {
                    System.out.println("无法创建Usage.java文件");
                    e.printStackTrace();
                }
            } else {
                System.out.println("Usage.java文件已存在");
            }

            File podFile = new File(podPath);
            if (!podFile.exists()) {
                // 创建LoggableAutoConfiguration.java文件
                try {
                    FileWriter writer = new FileWriter(podFile);
                    writer.write(getPodInfoContent(relativePath));
                    writer.close();
                    System.out.println("PodInfo.java文件已创建");
                } catch (IOException e) {
                    System.out.println("无法创建PodInfo.java文件");
                    e.printStackTrace();
                }
            } else {
                System.out.println("PodInfo.java文件已存在");
            }

            File containerFile = new File(containerPath);
            if (!containerFile.exists()) {
                // 创建LoggableAutoConfiguration.java文件
                try {
                    FileWriter writer = new FileWriter(containerFile);
                    writer.write(getContainersItemContent(relativePath));
                    writer.close();
                    System.out.println("ContainersItem.java文件已创建");
                } catch (IOException e) {
                    System.out.println("无法创建ContainersItem.java文件");
                    e.printStackTrace();
                }
            } else {
                System.out.println("ContainersItem.java文件已存在");
            }


        }


    }

    public List<String> getApplicationYamlOrPropertities(String servicesDirectory) throws IOException {
        Path start= Paths.get(servicesDirectory);
        List<String> applicationYamlOrProperities = new ArrayList<>();
        int maxDepth = 10;
        Stream<Path> stream = Files.find(start,maxDepth,(filepath, attributes) -> true);
        applicationYamlOrProperities = stream.sorted().map(String::valueOf).filter(filepath ->{
            if((String.valueOf(filepath).toLowerCase().endsWith("application.yml") || String.valueOf(filepath).toLowerCase().endsWith("application-dev.yml") || String.valueOf(filepath).toLowerCase().endsWith("application.yaml") || String.valueOf(filepath).toLowerCase().endsWith("application.properties") || String.valueOf(filepath).toLowerCase().endsWith("bootstrap.yml") || String.valueOf(filepath).toLowerCase().endsWith("bootstrap.properties")) && !String.valueOf(filepath).toLowerCase().contains("\\test\\") && !String.valueOf(filepath).toLowerCase().contains("/test/") && !String.valueOf(filepath).toLowerCase().contains("target") && !String.valueOf(filepath).toLowerCase().contains("target")){
                return true;
            }
            else {
                return false;
            }
        }).collect(Collectors.toList());
        return  applicationYamlOrProperities;
    }





    public boolean isControllerFileExists(String servicesDirectory) throws IOException {
        Path start = Paths.get(servicesDirectory);
        int maxDepth = 10;
        Stream<Path> stream = Files.find(start, maxDepth, (filePath, attributes) -> true);

        return stream
                .map(Path::getFileName)
                .map(Path::toString)
                .anyMatch(fileName -> { return fileName.toLowerCase().contains("controller") || fileName.toLowerCase().contains("web");});
    }

    public List<String> getStaticFiles(String servicesDirectory) throws IOException {
        Path start= Paths.get(servicesDirectory);
        List<String> staticFileList = new ArrayList<>();
        int maxDepth = 15;
        Stream<Path> stream = Files.find(start,maxDepth,(filepath, attributes) -> (String.valueOf(filepath).contains("html") ||String.valueOf(filepath).contains("js")));

        staticFileList = stream.sorted().map(String::valueOf).filter(filepath ->{
            if(String.valueOf(filepath).contains("\\resources\\") || String.valueOf(filepath).contains("/resources/")){
                return true;
            }
            else{
                return false;
            }
        }).collect(Collectors.toList());
        return  staticFileList;
    }
    public List<String> getJarFiles(String servicesDirectory) throws IOException {
        Path start= Paths.get(servicesDirectory);
        List<String> jarFiles = new ArrayList<>();
        int maxDepth = 10;
        Stream<Path> stream = Files.find(start,maxDepth,(filepath, attributes) -> String.valueOf(filepath).contains(".jar"));

        jarFiles = stream.sorted().map(String::valueOf).filter(filepath ->{
            if(String.valueOf(filepath).toLowerCase().contains(".jar.original") || String.valueOf(filepath).toLowerCase().contains("wrapper")){
                return false;
            }
            else {
                return true;
            }
        }).collect(Collectors.toList());
        return  jarFiles;
    }


    public String getPackageName(String servicesDirectory) throws IOException {
        Path start= Paths.get(servicesDirectory);
        List<String> javaFiles;
        int maxDepth = 10;
        String packageName = "";
        List<String> javaFilePaths = Files.find(start, maxDepth, (filepath, attributes) -> String.valueOf(filepath).endsWith("Application.java"))
                .map(Path::toString)
                .collect(Collectors.toList());
        for(String filepath : javaFilePaths){
            if(filepath.contains("/src/main/java/"))
                packageName = filepath.substring(filepath.indexOf("java/") + 5, filepath.lastIndexOf("/")).replace('/','.');
                break;
        }
        return packageName;
    }

//    public List<String> getJavaFiles(String servicesDirectory) throws IOException {
//        Path start= Paths.get(servicesDirectory);
//        List<String> javaFiles = new ArrayList<>();
//        int maxDepth = 10;
//        Stream<Path> stream = Files.find(start,maxDepth,(filepath, attributes) -> String.valueOf(filepath).contains(".java"));
//        javaFiles = stream.sorted().map(String::valueOf).collect(Collectors.toList());
//        return  javaFiles;
//    }
public static List<String> getJavaFiles(String servicesDirectory) throws IOException {
    Path start = Paths.get(servicesDirectory);
    List<String> javaFiles;
    int maxDepth = 15;
    Stream<Path> stream = Files.find(start, maxDepth, (filepath, attributes) -> String.valueOf(filepath).endsWith(".java"));
    //ignore .java files in package com.test,but classes outside this package can have "com.test" or "Test" in their names
    javaFiles = stream.sorted().map(String::valueOf)
            .filter(filepath ->
                    (!String.valueOf(filepath).contains("\\com.test\\") && !String.valueOf(filepath).contains("/test/"))
            )
            .collect(Collectors.toList());
    return javaFiles;
}
    public static List<String> getApplicationPath(String servicesDirectory) throws IOException {
        Path start= Paths.get(servicesDirectory);
        List<String> javaFiles;
        int maxDepth = 15;
        Stream<Path> stream = Files.find(start,maxDepth,(filepath, attributes) -> String.valueOf(filepath).endsWith("ApplicationMain.java"));
        //ignore .java files in package com.test,but classes outside this package can have "com.test" or "Test" in their names
        javaFiles= stream.sorted().map(String::valueOf)
                .filter(filepath ->
                        (!String.valueOf(filepath).contains("\\com.test\\") && !String.valueOf(filepath).contains("/test/"))
                )
                .collect(Collectors.toList());
        return javaFiles;
    }
    public static boolean findPomFiles(String directory) throws IOException {
        // 创建路径对象
        Path start = Paths.get(directory);

        // 定义搜索的最大深度
        int maxDepth = 10;

        // 使用 Files.find 查找文件
        Stream<Path> stream = Files.find(
                start,
                maxDepth,
                (filepath, attributes) -> String.valueOf(filepath).endsWith("pom.xml")
        );

        // 过滤路径（如果有特定规则可以在此添加）
        List<String> pomFiles = stream
                .sorted() // 排序路径
                .map(String::valueOf) // 转换为字符串
                .collect(Collectors.toList());

        return !pomFiles.isEmpty();
    }
    public static List<String> getServicePaths(String servicesDirectory) throws IOException {
        File dir = new File(servicesDirectory);
        FileFactory fileFactory = new FileFactory();
        List<String> servicesPath = new ArrayList<>();

        // Helper method for recursive search
        findServicePaths(dir, fileFactory, servicesPath);

        return servicesPath;
    }

    private static void findServicePaths(File directory, FileFactory fileFactory, List<String> servicesPath) throws IOException {
        File[] files = directory.listFiles();

        if (files == null) return; // If directory is empty or inaccessible

        for (File file : files) {
            String path = file.getAbsolutePath();

            // Check if it's a valid service directory
//            if (file.isDirectory() && !fileFactory.getApplicationYamlOrPropertities(path).isEmpty() && (path.toLowerCase().endsWith("service") || path.toLowerCase().endsWith("service-backend") || path.toLowerCase().endsWith("portal") || path.toLowerCase().endsWith("biz") || path.toLowerCase().endsWith("-common")) ) {
            System.out.println("path" +path);
            System.out.println("findPomFiles(path)" +findPomFiles(path));
            System.out.println("!fileFactory.getApplicationYamlOrPropertities(path).isEmpty()" +!fileFactory.getApplicationYamlOrPropertities(path).isEmpty());
            System.out.println("file.isDirectory()" +file.isDirectory());
            if (file.isDirectory() && !fileFactory.getApplicationYamlOrPropertities(path).isEmpty() && findPomFiles(path)) {
                servicesPath.add(path);
            } else if (file.isDirectory()) {
                // Recurse into subdirectories
                findServicePaths(file, fileFactory, servicesPath);
            }
        }
    }

//    public static List<String> getServicePaths(String servicesDirectory) throws IOException {
//        File dir = new File(servicesDirectory);
//        FileFactory fileFactory = new FileFactory();
//        File[] files = dir.listFiles();
//        List<String> servicesPath = new ArrayList<>();
//
//        for(File file: files){
//            String path = file.getAbsolutePath();
//            if(file.isDirectory() &&  !fileFactory.getApplicationYamlOrPropertities(path).isEmpty() && path.toLowerCase().endsWith("service")){
//                servicesPath.add(path);
//            }
//
//        }
//        return  servicesPath;
//    }

    public static String getServiceName(String servicePath) throws IOException {
        FileFactory fileFactory = new FileFactory();
        List<String> applicationYamlOrPropertities = fileFactory.getApplicationYamlOrPropertities(servicePath);
        String serviceName = null;
        for (String app : applicationYamlOrPropertities) {
            System.out.println(app);
            if (app.contains("application.")) {
                if (app.endsWith("yaml") || app.endsWith("yml")) {
                    Yaml yaml = new Yaml();
                    Map map = yaml.load(new FileInputStream(app));
                    Map m1 = (Map) map.get("spring");
                    Map m2 = (Map) m1.get("application");
                    serviceName = (String) m2.get("name");
                } else {
                    InputStream in = new BufferedInputStream(new FileInputStream(app));
                    Properties p = new Properties();
                    p.load(in);
                    serviceName = (String) p.get("spring.application.name");
                }
            }

        }
        return serviceName;
    }

    public static Map<String, String> getServiceDetails(String servicePath) throws IOException {
        FileFactory fileFactory = new FileFactory();
        List<String> applicationYamlOrPropertities = fileFactory.getApplicationYamlOrPropertities(servicePath);
        System.out.println("------------"+applicationYamlOrPropertities.toString());
        String serviceName = null;
        Integer servicePort = null;
        Integer defaultPort = 8080;
        Map<String,String> serviceDetail = new HashMap<>();
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);

        for (String app : applicationYamlOrPropertities) {
            if(app.contains("application") || app.contains("bootstrap")){
                System.out.println(app);
                if (app.endsWith("application.yaml") || app.endsWith("application.yml")  || app.endsWith("bootstrap.yml")  || app.endsWith("application-dev.yml")) {

                    Iterable<Object> allDocuments = yaml.loadAll(new FileInputStream(app));
                    Object firstDocument = null;
                    for (Object document : allDocuments) {
                        Map map = (Map) document; // 假设文档结构是 Map
                        Map m1 = (Map) map.get("spring");
                        Map m2 = (Map) m1.get("application");
                        if (m2 == null)
                            continue;
                        serviceName = (String) m2.get("name");
                        Map mServer = (Map) map.get("server");
                        if (mServer != null)
                            servicePort = mServer.get("port") != null ? (Integer) mServer.get("port") : defaultPort;
                        break;

                    }
                } else{
                    InputStream in = new BufferedInputStream(new FileInputStream(app));
                    Properties p = new Properties();
                    p.load(in);
                    serviceName = (String) p.get("spring.application.name");
                    servicePort =p.get("server.port") != null ?Integer.parseInt(p.get("server.port").toString()): defaultPort;
                }
                serviceDetail.put("serviceName", serviceName);
                serviceDetail.put("servicePort", String.valueOf(servicePort));
                System.out.println("serviceName" +serviceName);
            }
            else {
                // 解析 YAML 文件，loadAll 返回的是 Iterable<Object>
                Iterable<Object> documents = yaml.loadAll(new FileInputStream(app));
                List<Map<String, Object>> documentList = new ArrayList<>();
                for (Object document : documents) {
                    documentList.add((Map<String, Object>) document);
                }
                // 遍历每个 YAML 文档
                for (Map<String, Object> document : documentList) {
                    // 检查文档的 kind 是否为 Deployment
                    String kind = (String) document.get("kind");
                    if ("Deployment".equals(kind)) {
                        // 获取 Deployment 的名称
                        Map<String, Object> metadata = (Map<String, Object>) document.get("metadata");
                        String name = (String) metadata.get("name");

                        // 获取 Deployment 的资源配置
                        Map<String, Object> spec = (Map<String, Object>) document.get("spec");
                        Map<String, Object> template = (Map<String, Object>) spec.get("template");
                        Map<String, Object> templateSpec = (Map<String, Object>) template.get("spec");

                        // 获取 containers 列表
                        List<Map<String, Object>> containers = (List<Map<String, Object>>) templateSpec.get("containers");

                        // 遍历容器，获取 CPU 和 memory 请求量
                        for (Map<String, Object> container : containers) {
                            Map<String, Object> resources = (Map<String, Object>) container.get("resources");
                            Map<String, Object> requests = (Map<String, Object>) resources.get("requests");

                            String cpuRequest = (String) requests.get("cpu");
                            String memoryRequest = (String) requests.get("memory");
                            // 打印 Deployment 名称和资源请求
                            System.out.println("Deployment Name: " + name);
                            System.out.println("CPU Request: " + cpuRequest);
                            System.out.println("Memory Request: " + memoryRequest);
                            System.out.println("----------------------------");
                            serviceDetail.put("cpuRequest", cpuRequest);
                            serviceDetail.put("memoryRequest", memoryRequest);
                            break;
                        }
                        break;
                    }
                }
            }
        }
        return serviceDetail;
    }

    public static String gatewayServiceExisted(String servicePath, String originalService, Map<String, String> pathDetails) throws IOException {
        FileFactory fileFactory = new FileFactory();
        List<String> applicationYamlOrPropertities = fileFactory.getApplicationYamlOrPropertities(servicePath);
        String serviceName = null;
        for (String app : applicationYamlOrPropertities) {
            System.out.println("app" +app);
            if (app.endsWith("yaml") || app.endsWith("yml") ) {
                Yaml yaml = new Yaml();
                Map map = yaml.load(new FileInputStream(app));
                Map mSpring = (Map) map.get("spring");
                if (mSpring != null) {
                    Map mApplication = (Map) mSpring.get("application");
                    if (mApplication != null)
                        serviceName = (String) mApplication.get("name");
                    else
                        return null;
                }
                // get route config
                if (serviceName.toLowerCase().contains("gateway")) {
                    Map mCloud = (Map) mSpring.get("cloud");
                    if (mCloud != null) {
                        Map mGateway = (Map) mCloud.get("gateway");
                        if (mGateway != null) {
                            List<Map<String, Object>> mRoutes = (List<Map<String, Object>>) mGateway.get("routes");
                            System.out.println(mRoutes);
                            boolean routeExists = false;
                            // 检查是否存在相应的路由
                            if (originalService != null) {
                                for (Map<String, Object> route : mRoutes) {
                                    String uri = (String) route.get("uri");
                                    if (uri != null && uri.contains(originalService)) {
                                        // 如果路由存在，则添加新的路径
                                        List<String> predicates = (List<String>) route.get("predicates");
                                        if (predicates != null) {
                                            for (Map.Entry<String, String> entry : pathDetails.entrySet()) {
                                                predicates.add("Path=" + entry.getValue() + "/**");
                                            }
                                        }
                                        routeExists = true;
                                        break;
                                    }
                                }
                            }
                            //删除原始路由
//                            if (mRoutes.removeIf(route -> {
//                                String uri = (String) route.get("uri");
//                                return uri != null && uri.equals("lb://" + originalService);
//                            })) {
                                // 添加新路由
                             List<Map<String, Object>> newRoutes = new ArrayList<>();
                            if (!routeExists) {
                                for (Map.Entry<String, String> entry : pathDetails.entrySet()) {
                                    newRoutes.add(createRouteMap(serviceName, "lb://" + entry.getKey(), entry.getValue() + "/**"));
                                }
                                mRoutes.addAll(newRoutes);
                            }

//                            }
                            // 写回文件
                            try (Writer writer = new FileWriter(app)) {
                                yaml.dump(map, writer);
                            }
                            return app.substring(0,app.indexOf("src")-1);
                        }
                    }
                }
            }
        }
        return null;
    }

    public static String gatewayServiceExistedBefore(String servicePath, String originalService, String serviceName1, String serviceName2, String url1, String url2) throws IOException {
        FileFactory fileFactory = new FileFactory();
        List<String> applicationYamlOrPropertities = fileFactory.getApplicationYamlOrPropertities(servicePath);
        String serviceName = null;
        for (String app : applicationYamlOrPropertities) {
            if (app.endsWith("yaml") || app.endsWith("yml") ) {
                Yaml yaml = new Yaml();
                Map map = yaml.load(new FileInputStream(app));
                Map mSpring = (Map) map.get("spring");
                Map mApplication = (Map) mSpring.get("application");
                serviceName = (String) mApplication.get("name");
                // get route config
                if (serviceName.toLowerCase().contains("gateway")) {
                    Map mCloud = (Map) mSpring.get("cloud");
                    if (mCloud != null) {
                        Map mGateway = (Map) mCloud.get("gateway");
                        if (mGateway != null) {
                            List<Map<String, Object>> mRoutes = (List<Map<String, Object>>) mGateway.get("routes");
                            System.out.println(mRoutes);

                            //删除原始路由
                            if (mRoutes.removeIf(route -> {
                                String uri = (String) route.get("uri");
                                return uri != null && uri.equals("lb://" + originalService);
                            })) {
                                // 添加新路由
                                List<Map<String, Object>> newRoutes = Arrays.asList(
                                        createRouteMap("newRoute1", "lb://" + serviceName1, url1 + "/**"),
                                        createRouteMap("newRoute2", "lb://" + serviceName2, url2 + "/**")
                                );
                                mRoutes.addAll(newRoutes);
                            }
                            // 写回文件
                            try (Writer writer = new FileWriter(app)) {
                                yaml.dump(map, writer);
                            }
                           return app.substring(0,app.indexOf("src")-1);
                        }
                    }
                }
            }
        }
        return null;
    }


    private static Map<String, Object> createRouteMap(String id, String uri, String path) {
        Map<String, Object> newRoute = new HashMap<>();
        newRoute.put("id", id);
        newRoute.put("uri", uri);
// 创建包含Map的列表
        List<String> predicatesList = new ArrayList<>();
        predicatesList.add("Path="+path);

        // 将predicates放入新的路由中
        newRoute.put("predicates", predicatesList);

        return newRoute;
    }

    public static String getProjectPath(String path) {
        int lastSlashIndex = path.lastIndexOf("\\");
        if (lastSlashIndex == -1) {
            lastSlashIndex = path.lastIndexOf("/");
        }

        if (lastSlashIndex != -1) {
            return path.substring(0, lastSlashIndex);
        } else {
            return path;
        }
    }
    public static String setSVCNameAndDBName(String servicePath, String newName,String newDBName) throws IOException {
        FileFactory fileFactory = new FileFactory();
        List<String> applicationYamlOrPropertities = fileFactory.getApplicationYamlOrPropertities(servicePath);
        String serviceName = null;
        String databaseURL = null;
        for (String app : applicationYamlOrPropertities) {
            if (app.endsWith("yaml") || app.endsWith("yml") ) {
                DumperOptions options = new DumperOptions();
                options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                options.setPrettyFlow(true);
                Yaml yaml = new Yaml(options);
                Map map = yaml.load(new FileInputStream(app));
                Map mspring = (Map) map.get("spring");
                Map mapplication = (Map) mspring.get("application");
                if (mapplication == null)
                    continue;
                mapplication.put("name", newName);
                if (newDBName != null) {
                    Map mdatasource = (Map) mspring.get("datasource");
                    databaseURL = (String) mdatasource.get("url");
                    System.out.println(databaseURL);
                    mdatasource.put("url", updateDatabaseName(databaseURL, newDBName));
                }
                try (OutputStream out = new FileOutputStream(app)) {
                    yaml.dump(map, new OutputStreamWriter(out));
                }
            } else {
                InputStream in = new BufferedInputStream(new FileInputStream(app));
                Properties p = new Properties();
                p.load(in);
                serviceName = (String) p.get("spring.application.name");
                databaseURL = (String) p.get("spring.datasource.url");
                p.setProperty("spring.application.name", newName);
                if (newDBName != null)
                    p.setProperty("spring.datasource.url", updateDatabaseName(databaseURL, newDBName));
                // 将更新后的属性写回到文件中
                try (OutputStream out = new FileOutputStream(app)) {
                    p.store(out, null);
                }
            }

        }
        return serviceName;
    }

    public static String updateDatabaseName(String url, String newDatabaseName) {
        int questionMarkIndex = url.indexOf("?");
        String queryParams = "";
        if (questionMarkIndex != -1) {
            queryParams = url.substring(questionMarkIndex);
            url = url.substring(0, questionMarkIndex);
        }

        String[] parts = url.split("/");
        for (String part: parts)
            System.out.println(part);
        if (parts.length >= 3) {
            int lastSlashIndex = url.lastIndexOf("/");
            if (lastSlashIndex != -1) {
                String oldDatabaseName = parts[parts.length - 1];
                return url.substring(0, lastSlashIndex + 1) + newDatabaseName + queryParams;
            }
        }
        return url;
    }
    public List<String> getServicesCount(String servicesDirectory) throws IOException {
        File dir = new File(servicesDirectory);
        File[] files = dir.listFiles();
        List<String> servicesPath = new ArrayList<>();
        for(File file: files){
            if(file.isDirectory() && getApplicationYamlOrPropertities(file.getAbsolutePath()).size() != 0){
                servicesPath.add(file.getAbsolutePath());
            }
        }
        return  servicesPath;
    }


    public static Map<String, List<String>> getSvcEntityMap(RMonitor monitor, String projectPath, Map<String, List<Node>>  callchainMap) throws IOException {
        List<String> servicePaths = getServicePaths(projectPath);
        Map<String, List<String>> svcEntityMap = new HashMap<>();
        Map<String, String> namePathMap = new HashMap<>();
        for (String servicePath: servicePaths){
            String serviceName = getServiceDetails(servicePath).get("serviceName");
            namePathMap.put(serviceName, servicePath);
            List<String> entityList = new ArrayList<>();
            List<Node> callchain = callchainMap.get(servicePath);
            for (Node node: callchain){
                if (node.getType().equals(LayerType.ENTITY)) {
                    entityList.add(node.getId());
                    System.out.println("node.getId()" +node.getId());
                }
            }
            svcEntityMap.put(serviceName, entityList);
        }
        System.out.println("svcEntityMap: "+svcEntityMap.toString());
        return svcEntityMap;
    }

    public static double getAvgEntitys(Map<String, List<String>> svcEntityMap, double averageSize){
        double variance = svcEntityMap.values().stream()
                .mapToInt(List::size)
                .mapToDouble(size -> Math.pow(size - averageSize, 2))  // 计算每个 size 与平均值差值的平方
                .average()  // 计算平方差的平均值 (方差)
                .orElse(0.0);  // 如果没有数据，返回 0

        double standardDeviation = Math.sqrt(variance);  // 方差的平方根即为标准差
        return standardDeviation;
    }
    public static Map<String, String> getNamePathMap(RMonitor monitor, String projectPath) throws IOException {
        List<String> servicePaths = getServicePaths(projectPath);
        System.out.println("servicePaths"+servicePaths.toString());
        Map<String, String> namePathMap = new HashMap<>();
        for (String servicePath: servicePaths){
            String serviceName = getServiceDetails(servicePath).get("serviceName");
            namePathMap.put(serviceName, servicePath);
        }
        return namePathMap;
    }

    public ServiceContext getServiceList(String serviceDirectory) throws IOException {

        ServiceContext serviceContext = new ServiceContext();
        String servicesDirectory = new File(serviceDirectory).getAbsolutePath();
        List<String> servicesPath = getServicePaths(servicesDirectory);
        for (String svc : servicesPath) {
            List<String> applicationYamlOrPropertities = this.getApplicationYamlOrPropertities(svc);
            Yaml yaml = new Yaml();
            String serviceName = "";
            if (applicationYamlOrPropertities.size() == 0)
                continue;
            for (String app : applicationYamlOrPropertities) {
                if (app.endsWith("yaml") || app.endsWith("yml")) {
                    Map map = yaml.load(new FileInputStream(app));
                    Map m1 = (Map) map.get("spring");
                    Map m2 = (Map) m1.get("application");
                    serviceName = (String) m2.get("name");
                } else {
                    InputStream in = new BufferedInputStream(new FileInputStream(app));
                    Properties p = new Properties();
                    p.load(in);
                    serviceName = (String) p.get("spring.application.name");
                }
            }
            serviceContext.getServiceList().add(serviceName);
        }
        return serviceContext;
    }

//    public static void addConfig(String servicePath){
//
//    }
    public static void mergeDependencies(String nanoPath, String normalPath) throws IOException {
    //将nanoPath下的pom文件中的依赖合并到normalPath中的pom文件中，只匹配groupId + artifactId，如果normalPath的pom文件中存在该依赖则不添加，不存在则添加
    }
    public static void mergePaths(String nanoPath, String normalPath) throws IOException {
        File nanoDir = new File(nanoPath);
        File normalDir = new File(normalPath);

        if (!nanoDir.isDirectory() || !normalDir.isDirectory()) {
            throw new IllegalArgumentException("Both paths must be directories.");
        }

        // 遍历 nanoPath 下的所有文件和目录
        for (File file : nanoDir.listFiles()) {
            if (file.isFile()) {
                if (file.getPath().contains("/target/") || file.getPath().contains("\\target\\"))
                    continue;
                // 排除 YAML 文件和 Spring Boot 启动类文件
                if (!file.getName().endsWith(".yaml") && !file.getName().endsWith(".yml") && !file.getName().endsWith(".xml") && !isSpringBootApplication(file)) {
                    // 构建目标路径，保留原先路径
                    Path targetPath = buildTargetPath(file, nanoDir, normalDir);
                    // 创建目标目录
                    Files.createDirectories(targetPath.getParent());
                    // 复制文件到 normalPath
                    Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } else if (file.isDirectory()) {

                // 如果是目录，递归合并
                mergePaths(file.getPath(), new File(normalDir, file.getName()).getPath());
            }
        }
    }

    private static Path buildTargetPath(File file, File nanoDir, File normalDir) {
        // 计算相对路径
        String relativePath = nanoDir.toURI().relativize(file.toURI()).getPath();
        // 返回目标路径
        return new File(normalDir, relativePath).toPath();
    }

    private static boolean isSpringBootApplication(File file) {
        // 检查文件中是否包含 @SpringBootApplication 注解
        try {
            System.out.println("PATH" + file.toPath());
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                if (line.contains("@SpringBootApplication")) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static  Map<String, Long> getSvcFiles(Map<String, String> namePathMap) throws IOException, InterruptedException {
        Map<String, Long> locResult = new HashMap<>();

        for (String servicePath : namePathMap.values()) {
            long fileCount = Files.walk(Paths.get(servicePath))
                    // 过滤出常规文件
                    .filter(Files::isRegularFile)
                    // 计算文件数量
                    .count();
            locResult.put(servicePath, fileCount);

        }
        return locResult;
    }
    public static Map<String, Integer> getSvcLocs(Map<String, String> namePathMap) throws IOException, InterruptedException {
        Map<String, Integer> locResult = new HashMap<>();

        for (String servicePath: namePathMap.values()) {
            // 构造 cloc 命令
            ProcessBuilder processBuilder = new ProcessBuilder("cloc", servicePath);
            processBuilder.redirectErrorStream(true);

            // 执行命令
            Process process = processBuilder.start();

            // 输出命令执行结果并查找代码行数
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            int codeLines = 0;

            while ((line = reader.readLine()) != null) {
                // 解析 cloc 输出，查找包含代码行数的行
                if (line.contains("SUM:")) {
                    // 例如输出行：SUM: 10 1000 1000 10000
                    String[] parts = line.split("\\s+"); // 根据空格分割
                    if (parts.length >= 5) {
                        // 代码行数是第三个字段（假设输出格式没有变化）
                        try {
                            codeLines = Integer.parseInt(parts[4]);
                        } catch (NumberFormatException e) {
                            System.err.println("Error parsing code lines");
                        }
                    }
                }
            }
            // 等待命令执行完成
            process.waitFor();
            System.out.println("servicePath" +servicePath);
            locResult.put(servicePath, codeLines);
            // 输出代码行数
            System.out.println("Total code lines: " + codeLines);
        }
        return locResult;
    }

    public static void deleteFilesInFolder(String folderPath, List<String> fileNames) throws IOException {
        System.out.println("fileNames"+ fileNames.toString());
        for (int i=0; i< fileNames.size(); i++){
            String fileName = fileNames.get(i);
            if(fileName.endsWith("Impl"))
                fileNames.add(fileName.substring(0,fileName.length() -4));
        }
        Path folder = Paths.get(folderPath);
        Files.walk(folder)
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    String targetName;
                    if (fileName.endsWith(".java")) {
                        int lastIndex = fileName.lastIndexOf('.');
                        targetName = fileName.substring(0, lastIndex);
                    }
                    else
                        targetName = fileName;
                    System.out.println("fileName:"+ fileName);
                    return fileNames.stream().anyMatch(name -> targetName.equals(name) || name.equals(targetName +"Impl"));
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        System.out.println("已删除文件: " + path.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
    public static void copyFolder(String sourceFolder, String targetFolder1, String targetFolder2) throws IOException {
        Path sourcePath = Paths.get(sourceFolder);
        Path targetPath1 = Paths.get(targetFolder1);
        Path targetPath2 = Paths.get(targetFolder2);

        // 创建目标文件夹
        Files.createDirectories(targetPath1);
        Files.createDirectories(targetPath2);

        // 复制文件夹内容到两个目标文件夹
        Files.walk(sourcePath)
                .forEach(source -> {
                    try {
                        Path destination1 = targetPath1.resolve(sourcePath.relativize(source));
                        Path destination2 = targetPath2.resolve(sourcePath.relativize(source));
                        Files.copy(source, destination1, StandardCopyOption.REPLACE_EXISTING);
                        Files.copy(source, destination2, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    public static void copyFolder(String sourceFolder, String targetFolder) throws IOException {
        Path sourcePath = Paths.get(sourceFolder);
        Path targetPath1 = Paths.get(targetFolder);


        // 创建目标文件夹
        Files.createDirectories(targetPath1);

        // 复制文件夹内容到两个目标文件夹
        Files.walk(sourcePath)
                .forEach(source -> {
                    try {
                        Path destination1 = targetPath1.resolve(sourcePath.relativize(source));
                        Files.copy(source, destination1, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
    public static long countServiceFiles(Path startPath) throws IOException {
        // 正则表达式用于匹配以 'service' 结尾的 .java 文件
        Pattern pattern = Pattern.compile(".*service\\.java$", Pattern.CASE_INSENSITIVE);

        return countFilesWithPattern(startPath, pattern);
    }
    public static long countControllerFiles(Path startPath) throws IOException {
        // 正则表达式用于匹配以 'controller' 结尾的 .java 文件
        Pattern pattern = Pattern.compile(".*controller\\.java$", Pattern.CASE_INSENSITIVE);

        return countFilesWithPattern(startPath, pattern);
    }

    // 统计符合给定模式的文件数量
    private static long countFilesWithPattern(Path startPath, Pattern pattern) throws IOException {
        // 使用 Files.walk() 遍历目录及子目录
        try (Stream<Path> paths = Files.walk(startPath)) {
            return paths
                    .filter(Files::isRegularFile) // 仅保留常规文件
                    .filter(path -> pattern.matcher(path.getFileName().toString()).matches()) // 筛选符合模式的 .java 文件
                    .count(); // 统计符合条件的文件数量
        }
    }
    // 计算一致性得分
    public static double calculateConsistencyScore(long serviceCount, long controllerCount) {
        // 计算差异的绝对值
        long difference = Math.abs(serviceCount - controllerCount);

        // 计算较大值
        long maxCount = Math.max(serviceCount, controllerCount);

        // 避免除以0的情况
        if (maxCount == 0) {
            return 1.0; // 如果两个数量都为0，认为是一致的
        }

        // 计算一致性得分
        return 1.0 - (double) difference / maxCount;
    }


    // 用于存放统计结果的内部类
    static class Result {
        int controllerCount;
        int serviceCount;

        Result(int controllerCount, int serviceCount) {
            this.controllerCount = controllerCount;
            this.serviceCount = serviceCount;
        }
    }

    public static Result countControllersAndServices(String path) {
        int controllerCount = 0;
        int serviceCount = 0;

        // 遍历文件夹中的所有文件和子文件夹
        File directory = new File(path);
        if (directory.exists() && directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    Result subDirResult = countControllersAndServices(file.getAbsolutePath());
                    controllerCount += subDirResult.controllerCount;
                    serviceCount += subDirResult.serviceCount;
                } else if (file.getName().endsWith(".java")) {
                    Result fileResult = countServicesInFile(file);
                    controllerCount += fileResult.controllerCount;
                    serviceCount += fileResult.serviceCount;
                }
            }
        }

        return new Result(controllerCount, serviceCount);
    }

    private static Result countServicesInFile(File file) {
        int serviceCount = 0;
        int controllerCount = 0;

        try {
            // 解析 Java 文件
            CompilationUnit cu = StaticJavaParser.parse(file);
            List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);

            for (ClassOrInterfaceDeclaration clazz : classes) {
                if (clazz.getNameAsString().endsWith("Controller")) {
                    controllerCount++;

                    // 统计以 'service' 结尾的字段
                    List<FieldDeclaration> fields = clazz.getFields();
                    for (FieldDeclaration field : fields) {
                        for (VariableDeclarator variable : field.getVariables()) {
                            if (variable.getNameAsString().toLowerCase().endsWith("service")) {
                                serviceCount++;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Result(controllerCount, serviceCount);
    }
    private static Set<String> getControllerClasses(String directoryPath) {
        Set<String> controllerClasses = new HashSet<>();
        File directory = new File(directoryPath);

        // 遍历文件夹中的所有文件和子文件夹
        if (directory.exists() && directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    controllerClasses.addAll(getControllerClasses(file.getAbsolutePath())); // 递归进入子目录
                } else if (file.getName().endsWith(".java")) {
                    // 解析 Java 文件
                    try {
                        CompilationUnit cu = StaticJavaParser.parse(file);
                        List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
                        for (ClassOrInterfaceDeclaration clazz : classes) {
                            if (clazz.getNameAsString().endsWith("Controller")) {
                                // 添加控制器类名到集合
                                controllerClasses.add(clazz.getNameAsString());
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return controllerClasses;
    }

    public static void main(String[] args) throws IOException, XmlPullParserException {


        Result result1 = countControllersAndServices("D:\\code\\projects\\refactor-projects\\PropertyManagementCloud-master\\PropertyManagementCloud-master\\user-service");
        System.out.println("控制器类的数量: " + result1.controllerCount);
        System.out.println("以 'service' 结尾的属性总数量: " + result1.serviceCount);
// 给定路径，可以根据需要修改路径
//        String path = "D:\\code\\projects\\refactor-projects\\apollo-master\\apollo-master\\apollo-portal_2"; // 替换为你的路径
        List<String> servicePaths = FileFactory.getServicePaths("D:\\code\\projects\\refactor-projects\\PropertyManagementCloud-master\\PropertyManagementCloud-master");
        System.out.println("servicePaths "+servicePaths.toString());
        List<String> newPaths = new ArrayList<>();
        for (String path: servicePaths){
            if (path.contains("D:\\code\\projects\\refactor-projects\\PropertyManagementCloud-master\\PropertyManagementCloud-master\\user-service")) {
                System.out.println("------");
                continue;
            }
            newPaths.add(path);
        }
//        newPaths.add("D:\\code\\projects\\refactor-projects\\PropertyManagementCloud-master\\PropertyManagementCloud-master\\user-service");
        newPaths.add("D:\\code\\projects\\refactor-projects\\PropertyManagementCloud-master\\PropertyManagementCloud-master\\user-service_1");
        newPaths.add("D:\\code\\projects\\refactor-projects\\PropertyManagementCloud-master\\PropertyManagementCloud-master\\user-service_2");
        System.out.println("newPath" + newPaths.toString());
        double consistencyScore = 0;
        for (String path :newPaths) {

//        String path1 = "D:\\code\\projects\\refactor-projects\\apollo-master\\apollo-master\\apollo-portal_1"; // 替换为你的路径
//        String path2 = "D:\\code\\projects\\refactor-projects\\apollo-master\\apollo-master\\apollo-portal_2"; // 替换为你的路径
//
//        Set<String> controllersInPath = getControllerClasses(path);
//        Set<String> controllersInPath1 = getControllerClasses(path1);
//        Set<String> controllersInPath2 = getControllerClasses(path2);
//
//        // 找到在 path 中存在但在 path1 和 path2 中不存在的控制器类
//        controllersInPath.removeAll(controllersInPath1);
//        controllersInPath.removeAll(controllersInPath2);
//
//        // 输出结果
//        System.out.println("在 path 中存在而在 path1 和 path2 中不存在的控制器类:");
//        for (String controller : controllersInPath) {
//            System.out.println(controller);
//        }
            System.out.println("path" +path);
            Result result = countControllersAndServices(path);
            System.out.println("控制器类的数量: " + result.controllerCount);
            System.out.println("以 'service' 结尾的属性总数量: " + result.serviceCount);
            System.out.println("consistencyScore" +consistencyScore);
            System.out.println("calculateConsistencyScore(result.serviceCount, result.controllerCount)" +calculateConsistencyScore(result.serviceCount, result.controllerCount));
            // 计算一致性得分
            consistencyScore = consistencyScore + calculateConsistencyScore(result.serviceCount, result.controllerCount);
        }
        System.out.println("Consistency score: " + (double)consistencyScore/servicePaths.size());
//            List<String> paths =new ArrayList<>();
//            paths.add("D:\\code\\demo-collection\\Service-Demo\\testService\\src\\main\\java\\org\\example\\ApplicationMain.java");
//            Set<String> set = new HashSet<>();
//            set.add("test-service");
//            set.add("travel-service");
//            DependencyUtils.modifyPomFile(new File(getPomFiles("D:\\code\\demo-collection\\Service-Demo\\testService").get(0)));
//            System.out.println(getPomFiles("D:\\code\\demo-collection\\Service-Demo\\testService").toString());
//            addConfig(paths, set, "test-service");
////        setSVCNameAndDBName("D:\\code\\Service-Demo\\testService1", "SERVICE_SPLIT", "DB_SPLT");
////        System.out.println(gatewayServiceExistedBefore("D:\\code\\gateway-demo-master\\gateway-demo-master", "service-demo","service-demo_split_1", "service-demo_split_2","/api/v1", "/api/v2"));
////          mergePaths("D:\\code\\demo-collection\\nano-test\\travelservice", "D:\\code\\demo-collection\\nano-test\\routeservice");
//
//            List<String> javaFiles = getApplicationPath("D:\\code\\demo-collection\\Service-Demo\\testService");
//            System.out.println(javaFiles.size());
//            // 假设你的 YAML 文件路径为 "k8s-deployment.yaml"
//            String filePath = "D:\\code\\demo-collection\\Service-Demo\\testService\\test-service.yaml";
//            DumperOptions options = new DumperOptions();
//            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
//            options.setPrettyFlow(true);
//            Yaml yaml = new Yaml(options);
//
//            // 解析 YAML 文件，loadAll 返回的是 Iterable<Object>
//            Iterable<Object> documents = yaml.loadAll(new FileInputStream(filePath));
//            List<Map<String, Object>> documentList = new ArrayList<>();
//            for (Object document : documents) {
//                documentList.add((Map<String, Object>) document);
//            }
//            // 遍历每个 YAML 文档
//            for (Map<String, Object> document : documentList) {
//                // 检查文档的 kind 是否为 Deployment
//                String kind = (String) document.get("kind");
//                if ("Deployment".equals(kind)) {
//                    // 获取 Deployment 的名称
//                    Map<String, Object> metadata = (Map<String, Object>) document.get("metadata");
//                    String name = (String) metadata.get("name");
//
//                    // 获取 Deployment 的资源配置
//                    Map<String, Object> spec = (Map<String, Object>) document.get("spec");
//                    Map<String, Object> template = (Map<String, Object>) spec.get("template");
//                    Map<String, Object> templateSpec = (Map<String, Object>) template.get("spec");
//
//                    // 获取 containers 列表
//                    List<Map<String, Object>> containers = (List<Map<String, Object>>) templateSpec.get("containers");
//
//                    // 遍历容器，获取 CPU 和 memory 请求量
//                    for (Map<String, Object> container : containers) {
//                        Map<String, Object> resources = (Map<String, Object>) container.get("resources");
//                        Map<String, Object> requests = (Map<String, Object>) resources.get("requests");
//
//                        String cpuRequest = (String) requests.get("cpu");
//                        String memoryRequest = (String) requests.get("memory");
//
//                        // 打印 Deployment 名称和资源请求
//                        System.out.println("Deployment Name: " + name);
//                        System.out.println("CPU Request: " + cpuRequest);
//                        System.out.println("Memory Request: " + memoryRequest);
//                        System.out.println("----------------------------");
//                    }
//                }
//            }
        }

    /**
     * 删除指定目录下的含有对应字符的文件或目录
     * @param directory 指定项目目录
     * @param pattern 删除文件或目录的匹配规则
     */
    public static void deleteFilesByPattern(String directory, String pattern) throws IOException {
        File[] files = new File(directory).listFiles();
        if (files != null) {
            for (File file: files) {
                if (file.isDirectory()) {
                    deleteFilesByPattern(file.getAbsolutePath(), pattern);
                } else {
                    if (file.getAbsolutePath().contains(pattern)) {
                        Files.delete(file.toPath());
                    }
                }
                if (file.isDirectory() && file.getAbsolutePath().contains(pattern)) {
                    Files.delete(file.toPath());
                }
            }
        }
    }

    /**
     * 更新 application.yaml 或 application.properties 文件
     * @param applicationPath 配置文件路径
     * @param discovery 服务发现组件类型
     * @param type 服务发现客户端或服务器
     */
    public static void updateApplicationYamlOrProperties(String applicationPath, String discovery, int type) throws IOException {
        if (applicationPath.endsWith("properties")) {
            YamlAndPropertiesParserUtils.updateApplicationProperties(applicationPath, discovery, type);
        } else {
            YamlAndPropertiesParserUtils.updateApplicationYaml(applicationPath, discovery, type);
        }
    }

    public static void updateApplicationYamlOrProperties(String applicationPath, TemplateFile template) throws IOException {
        if (applicationPath.endsWith("properties")) {
            YamlAndPropertiesParserUtils.updateApplicationProperties(applicationPath, template);
        } else {
            YamlAndPropertiesParserUtils.updateApplicationYaml(applicationPath, template);
        }
    }

    public static void updateApplicationYamlOrProperties(String applicationPath, Map<String, Object> configurations) throws IOException {
        if (applicationPath.endsWith("properties")) {
            YamlAndPropertiesParserUtils.updateApplicationProperties(applicationPath, configurations);
        } else {
            YamlAndPropertiesParserUtils.updateApplicationYaml(applicationPath, configurations);
        }
    }

    public static List<String> getPomXmlPaths(String directory) throws IOException {
        Path path = Paths.get(directory);
        int maxDepth = 10;
        Stream<Path> stream = Files.find(path, maxDepth, (filepath, attributes) -> String.valueOf(filepath).contains("pom.xml"));
        return stream.sorted().map(String::valueOf).filter(filepath -> !String.valueOf(filepath).toLowerCase().contains(".mvn")
                && !String.valueOf(filepath).toLowerCase().contains("gradle")).collect(Collectors.toList());
    }

    public static Map<String, String> getFilePathToMicroserviceName(String directory) throws IOException {
        Map<String, String> filePathToMicroserviceName = new LinkedHashMap<>();
        List<String> services = getServices(directory);
        for (String filePath: services) {
            List<String> applicationYamlOrProperties = getApplicationYamlOrPropertiesPaths(filePath);
            Map<String, Object> configurations = new LinkedHashMap<>();
            String microserviceName = "";
            for (String applicationYamlOrProperty: applicationYamlOrProperties) {
                if (applicationYamlOrProperty.endsWith("yml") || applicationYamlOrProperty.endsWith("yaml")) {
                    Iterable<Object> objects = yamlReader.loadAll(Files.newInputStream(Paths.get(applicationYamlOrProperty)));
                    for (Object o: objects) {
                        YamlAndPropertiesParserUtils.resolveYaml(new Stack<>(), configurations, (Map<String, Object>) o);
                    }
                } else {
                    YamlAndPropertiesParserUtils.resolveProperties(applicationYamlOrProperty, configurations);
                }
                String[] strings = filePath.split("/|\\\\");
                microserviceName = configurations.getOrDefault("spring.application.name", strings[strings.length - 1]).toString();
            }
            filePathToMicroserviceName.put(filePath, microserviceName);
        }
        return filePathToMicroserviceName;
    }

    public static String getMicroserviceName(String applicationPath) throws IOException {
        Map<String, Object> configurations = new LinkedHashMap<>();
        String microserviceName;
        if (applicationPath.equals("yml") || applicationPath.equals("yaml")) {
            Iterable<Object> objects = yamlReader.loadAll(Files.newInputStream(Paths.get(applicationPath)));
            for (Object o: objects) {
                YamlAndPropertiesParserUtils.resolveYaml(new Stack<>(), configurations, (Map<String, Object>) o);
            }
        } else {
            YamlAndPropertiesParserUtils.resolveProperties(applicationPath, configurations);
        }
        microserviceName = configurations.getOrDefault("spring.application.name", "").toString();
        return microserviceName;
    }

    public static Map<String, String> getFilePathToMicroservicePort(String directory) throws IOException {
        Map<String, String> filePathToMicroservicePort = new LinkedHashMap<>();
        List<String> services = getServices(directory);
        for (String filePath: services) {
            List<String> applicationYamlOrProperties = getApplicationYamlOrPropertiesPaths(filePath);
            Map<String, Object> configurations = new LinkedHashMap<>();
            String port = "8080";
            for (String applicationYamlOrProperty: applicationYamlOrProperties) {
                if (applicationYamlOrProperty.endsWith("yml") || applicationYamlOrProperty.endsWith("yaml")) {
                    Iterable<Object> objects = yamlReader.loadAll(Files.newInputStream(Paths.get(applicationYamlOrProperty)));
                    for (Object o: objects) {
                        YamlAndPropertiesParserUtils.resolveYaml(new Stack<>(), configurations, (Map<String, Object>) o);
                    }
                } else {
                    YamlAndPropertiesParserUtils.resolveProperties(applicationYamlOrProperty, configurations);
                }
                port = configurations.getOrDefault("server.port", port).toString();
            }
            filePathToMicroservicePort.put(filePath, port);
        }
        return filePathToMicroservicePort;
    }

    public static List<String> getApplicationYamlOrPropertiesPaths(String directory) throws IOException {
        Path parent = Paths.get(directory);
        int maxDepth = 10;
        Stream<Path> stream = Files.find(parent, maxDepth, (filePath, attributes) -> true);
        return stream.sorted().map(String::valueOf).filter(filePath -> (String.valueOf(filePath).toLowerCase().endsWith("application.yml")
                || String.valueOf(filePath).toLowerCase().endsWith("application.yaml")
                || String.valueOf(filePath).toLowerCase().endsWith("application.properties")
                || String.valueOf(filePath).toLowerCase().endsWith("bootstrap.yml"))
                && !String.valueOf(filePath).toLowerCase().contains("target")).collect(Collectors.toList());
    }

    public static List<String> getServices(String directory) throws IOException {
        File[] files = new File(directory).listFiles();
        List<String> services = new LinkedList<>();
        if (files != null) {
            for (File file: files) {
                if (file.isDirectory()) {
                    if (file.toString().contains("src")) {
                        boolean flag = false;
                        List<String> javaFiles = getJavaFiles(file.toString());
                        for (String javaFile : javaFiles) {
                            if (JavaParserUtils.isStartupClass(javaFile)) {
                                flag = true;
                                break;
                            }
                        }
                        if (flag) {
                            services.add(file.toString().substring(0, file.toString().lastIndexOf("src")));
                        }
                    } else {
                        services.addAll(getServices(file.toString()));
                    }
                }
            }
        }
        return services;
    }

    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        return file.delete();
    }

    public static String createFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.createNewFile()) {
            return file.getAbsolutePath();
        }
        return null;
    }

    public static String createDirectory(String filePath) {
        File file = new File(filePath);
        if (file.mkdirs()) {
            return file.getAbsolutePath();
        }
        return null;
    }

    public static boolean isFileExists(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path) && Files.isRegularFile(path);
    }
}

