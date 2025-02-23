package com.refactor.utils;

import com.refactor.config.GatewayConfig;
import com.refactor.config.TemplatesConfig;
import com.refactor.config.helper.ConfigHelper;
import com.refactor.enumeration.TemplateFile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.BufferedInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Cocoicobird
 * @version 1.0
 */
@Component
public class YamlAndPropertiesParserUtils {

    private static final TemplatesConfig templatesConfig = ConfigHelper.getTemplatesConfig();
    private static final Map<String, Object> cache = new LinkedHashMap<>();
    private static final Yaml yamlReader = new Yaml();
    private static final Yaml yamlWriter;

    static {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        options.setPrettyFlow(true);
        yamlWriter = new Yaml(options);
    }

    /**
     * 解析 yaml 里的数据数据存入到 map 中
     * @param stack 存储 key 的栈
     * @param map 用于存储配置文件 key-value 的映射
     * @param yaml 读取的待解析的 yaml 文件内容
     */
    public static void resolveYaml(Stack<String> stack, Map<String, Object> map, Map<String, Object> yaml) {
        for (String key : yaml.keySet()) {
            Object value = yaml.get(key);
            stack.add(key);
            if (value instanceof Map) {
                resolveYaml(stack, map, (Map<String, Object>) value);
            } else {
                map.put(String.join(".", stack), value == null ? "" : value);
                stack.pop();
            }
        }
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    /**
     * 更新 application.yaml 文件
     * @param applicationYamlPath application.yaml 文件路径
     * @param discovery 服务发现组件名称
     * @param type 0: client 1: server
     */
    public static void updateApplicationYaml(String applicationYamlPath, String discovery, int type) throws IOException {
        // 加载配置模板文件的配置项
        Map<String, Object> properties = resolveTemplateYaml(discovery, type);
        Iterable<Object> originProperties = yamlReader.loadAll(Files.newInputStream(Paths.get(applicationYamlPath)));
        for (Object o : originProperties) {
            resolveYaml(new Stack<>(), properties, (Map<String, Object>) o);
        }
        // 模板文件配置项转换为嵌套 Map
        Map<String, Object> yaml = transMapToMap(properties);
        try (FileWriter writer = new FileWriter(applicationYamlPath)){
            writer.write(yamlWriter.dump(yaml));
        }
    }

    /**
     * 将模板文件中的内容更新至 application.yaml 文件
     * @param applicationYamlPath 待更新的配置文件
     * @param template 模板文件
     */
    public static void updateApplicationYaml(String applicationYamlPath, TemplateFile template) throws IOException {
        Map<String, Object> properties = resolveSpecifiedTemplateYaml(template);
        Iterable<Object> originProperties = yamlReader.loadAll(Files.newInputStream(Paths.get(applicationYamlPath)));
        for (Object o: originProperties) {
            resolveYaml(new Stack<>(), properties, (Map<String, Object>) o);
        }
        // 模板文件配置项转换为嵌套 Map
        Map<String, Object> yaml = transMapToMap(properties);
        try (FileWriter writer = new FileWriter(applicationYamlPath)){
            writer.write(yamlWriter.dump(yaml));
        }
    }

    public static void updateApplicationYaml(String applicationYamlPath, Map<String, Object> properties) throws IOException {
        Iterable<Object> objects = yamlReader.loadAll(Files.newInputStream(Paths.get(applicationYamlPath)));
        Map<String, Object> yaml = new LinkedHashMap<>();
        for (Object o: objects) {
            resolveYaml(new Stack<>(), yaml, (Map<String, Object>) o);
        }
        resolveYaml(new Stack<>(), yaml, properties);
        try (FileWriter writer = new FileWriter(applicationYamlPath)){
            writer.write(yamlWriter.dump(transMapToMap(yaml)));
        }
    }

    private static Map<String, Object> transMapToMap(Map<String, Object> properties) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : properties.keySet()) {
            Map<String, Object> m = transStringToMap(key, properties.get(key));
            mergeYaml(map, m);
        }
        return map;
    }

    public static Map<String, Object> transStringToMap(String key, Object value) {
        int index = key.indexOf('.');
        if (index < 0) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(key, value);
            return map;
        }
        String k = key.substring(0, index);
        String v = key.substring(index + 1);
        Map<String, Object> m = transStringToMap(v, value);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put(k, m);
        return map;
    }

    /**
     * 合并两个 map
     * @param origin 原始读取的 yaml map
     * @param add 需要添加的 yaml map
     */
    public static void mergeYaml(Map<String, Object> origin, Map<String, Object> add) {
        for (String addKey : add.keySet()) {
            boolean duplicated = false;
            for (String originKey : origin.keySet()) {
                if (addKey.equals(originKey)) {
                    duplicated = true;
                    Object addValue = add.get(addKey);
                    Object originValue = origin.get(originKey);
                    mergeYaml((Map<String, Object>) originValue, (Map<String, Object>) addValue);
                    break;
                }
            }
            if (!duplicated) {
                origin.put(addKey, add.get(addKey));
            }
        }
    }

    /**
     * 解析 properties 文件
     * @param applicationPropertiesPath properties 文件路径
     * @param map 存储配置文件 key-value 的映射
     */
    public static void resolveProperties(String applicationPropertiesPath, Map<String, Object> map) throws IOException {
        Properties properties = new Properties();
        properties.load(new BufferedInputStream(Files.newInputStream(Paths.get(applicationPropertiesPath))));
        for (Object key : properties.keySet()) {
            map.put((String) key, properties.getProperty((String) key));
        }
    }

    /**
     * 更新 application.properties 文件
     */
    public static void updateApplicationProperties(String applicationPropertiesPath, String discovery, int type) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        resolveProperties(applicationPropertiesPath, map);
        Map<String, Object> properties = resolveTemplateYaml(discovery, type);
        for (String key : properties.keySet()) {
            map.put(key, properties.get(key).toString());
        }
        try (FileWriter writer = new FileWriter(applicationPropertiesPath)){
            for (String key : map.keySet()) {
                writer.write(key + '=' + map.get(key) + "\n");
            }
        }
    }

    /**
     * 将模板文件中的内容更新至 application.properties 文件
     * @param applicationPropertiesPath 待更新的配置文件
     * @param template 模板文件
     */
    public static void updateApplicationProperties(String applicationPropertiesPath, TemplateFile template) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        resolveProperties(applicationPropertiesPath, map);
        Map<String, Object> properties = resolveSpecifiedTemplateYaml(template);
        for (String key : properties.keySet()) {
            map.put(key, properties.get(key).toString());
        }
        try (FileWriter writer = new FileWriter(applicationPropertiesPath)){
            for (String key : map.keySet()) {
                writer.write(key + '=' + map.get(key) +"\n");
            }
        }
    }

    public static void updateApplicationProperties(String applicationPropertiesPath, Map<String, Object> properties) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        resolveProperties(applicationPropertiesPath, map);
        resolveYaml(new Stack<>(), map, properties);
        System.out.println(map);
        try (FileWriter writer = new FileWriter(applicationPropertiesPath)){
            for (String key : map.keySet()) {
                writer.write(key + '=' + map.get(key) +"\n");
            }
        }
    }

    /**
     * 解析模板文件
     */
    private static Map<String, Object> resolveTemplateYaml(String discovery, int type) throws IOException {
        Map<String, Object> properties = new LinkedHashMap<>();
        String templatePath = "eureka".equals(discovery)
                ? (type == 0 ? templatesConfig.getEurekaClient() : templatesConfig.getEurekaServer())
                : templatesConfig.getNacosClient();
        Resource resource = new ClassPathResource(templatePath);
        Iterable<Object> templateConfiguration = yamlReader.loadAll(resource.getInputStream());
        cache.clear();
        for (Object o : templateConfiguration) {
            YamlAndPropertiesParserUtils.resolveYaml(new Stack<>(), cache, (Map<String, Object>) o);
        }
        properties.putAll(cache);
        return properties;
    }

    /**
     * 解析指定的模板文件
     * @param template 目标模板
     */
    public static Map<String, Object> resolveSpecifiedTemplateYaml(TemplateFile template) throws IOException {
        Map<String, Object> properties = new LinkedHashMap<>();
        String templatePath = "";
        if (TemplateFile.CONFIG_CLIENT == template) {
            templatePath = templatesConfig.getConfigClient();
        } else if (TemplateFile.CONFIG_SERVER == template) {
            templatePath = templatesConfig.getConfigServer();
        } else if (TemplateFile.EUREKA_CLIENT == template) {
            templatePath = templatesConfig.getEurekaClient();
        } else if (TemplateFile.EUREKA_SERVER == template) {
            templatePath = templatesConfig.getEurekaServer();
        } else if (TemplateFile.NACOS_CLIENT == template) {
            templatePath = templatesConfig.getNacosClient();
        } else if (TemplateFile.GATEWAY == template) {
            templatePath = templatesConfig.getGateway();
        }
        Resource resource = new ClassPathResource(templatePath);
        Iterable<Object> templateConfiguration = yamlReader.loadAll(resource.getInputStream());
        cache.clear();
        for (Object o : templateConfiguration) {
            YamlAndPropertiesParserUtils.resolveYaml(new Stack<>(), cache, (Map<String, Object>) o);
        }
        properties.putAll(cache);
        return properties;
    }

    /**
     * 去除自定义类的标签
     */
    public static Map<String, Object> removeCustomClasses(Map<String, Object> map, Class<?> c) {
        Constructor constructor = new Constructor(c);
        TypeDescription typeDescription = new TypeDescription(c);
        constructor.addTypeDescription(typeDescription);
        Representer representer = new Representer();
        representer.addClassTag(c, Tag.MAP);
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(constructor, representer, options);
        Yaml loader = new Yaml();
        return loader.load(yaml.dump(map));
    }

    /**
     * 网关配置
     * @param microserviceNameToPreUrls 微服务名称与 url 前缀的映射
     */
    public static Map<String, Object> getSpringCloudGateway(Map<String, Set<String>> microserviceNameToPreUrls) throws Exception {
        Map<String, Object> springCloudGateway = new LinkedHashMap<>();
        List<GatewayConfig> routes = new LinkedList<>();
        for (String microserviceName: microserviceNameToPreUrls.keySet()) {
            Set<String> preUrls = microserviceNameToPreUrls.get(microserviceName);
            String paths = preUrls.stream().map(preUrl -> preUrl + "/**").collect(Collectors.joining(","));
            GatewayConfig gatewayConfig = new GatewayConfig(microserviceName, "lb://" + microserviceName, Arrays.asList("Path=" + paths));
            routes.add(gatewayConfig);
        }
        springCloudGateway.put("routes", routes);
        Map<String, Object> springCloud = new LinkedHashMap<>();
        springCloud.put("gateway", springCloudGateway);
        Map<String, Object> spring = new LinkedHashMap<>();
        spring.put("cloud", springCloud);
        Map<String, Object> yaml = new LinkedHashMap<>();
        yaml.put("spring", spring);
        return yaml;
    }

    /**
     * Config Center 客户端配置服务名称
     */
    public static Map<String, Object> getConfigClient(String microserviceName) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("name", microserviceName);
        Map<String, Object> cloud = new LinkedHashMap<>();
        cloud.put("config", config);
        Map<String, Object> spring = new LinkedHashMap<>();
        spring.put("cloud", cloud);
        Map<String, Object> yaml = new LinkedHashMap<>();
        yaml.put("spring", spring);
        return yaml;
    }

    /**
     * 获取当前微服务模块的配置
     */
    public static Map<String, Object> getConfigurations(String microservicePath) throws IOException {
        Map<String, Object> configurations = new LinkedHashMap<>();
        Yaml yamlReader = new Yaml();
        List<String> applicationYamlAndProperties = FileFactory.getApplicationYamlOrPropertiesPaths(microservicePath);
        for (String applicationYamlAndProperty: applicationYamlAndProperties) {
            if (applicationYamlAndProperty.endsWith("yml") || applicationYamlAndProperty.endsWith("yaml")) {
                Iterable<Object> objects = yamlReader.loadAll(Files.newInputStream(Paths.get(applicationYamlAndProperty)));
                for (Object o: objects) {
                    YamlAndPropertiesParserUtils.resolveYaml(new Stack<>(), configurations, (Map<String, Object>) o);
                }
            } else if (applicationYamlAndProperty.endsWith("properties")) {
                YamlAndPropertiesParserUtils.resolveProperties(applicationYamlAndProperty, configurations);
            }
        }
        return configurations;
    }
}
