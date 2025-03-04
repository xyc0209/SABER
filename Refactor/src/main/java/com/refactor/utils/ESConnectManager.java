package com.refactor.utils;

/**
 * @description:
 * @author: xyc
 * @date: 2025-02-25 16:05
 */
import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.HealthReportResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.refactor.dto.ElasticsearchSettings;
import com.refactor.dto.Host;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ESConnectManager {
    private static final Logger logger = LoggerFactory.getLogger(ESConnectManager.class);
    public ElasticsearchClient client = null;
    public ElasticsearchAsyncClient asyncClient = null;
    @Autowired
    private ElasticsearchSettings elasticsearchSettings;

    private synchronized void initializeClient() {

        if (client != null || asyncClient != null) {
            close(); // 关闭现有连接
        }
        System.out.println(elasticsearchSettings.toString());
        List<Host> hosts = parseAddresses(elasticsearchSettings.getAddresses());

        final CredentialsProvider credentialsProvider =
                new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(elasticsearchSettings.getUserName(), elasticsearchSettings.getPassword()));


        List<HttpHost> httpHosts = hosts.stream().map(host -> new HttpHost(host.getHostName(), host.getPort()))
                .collect(Collectors.toList());

        RestClientBuilder builder = RestClient.builder(httpHosts.toArray(new HttpHost[httpHosts.size()]))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(
                            HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder
                                .setDefaultCredentialsProvider(credentialsProvider);
                    }
                });

        RestClient restClient = builder.build();


        //配置使得Java对象中不存在的属性，json对象解析时直接忽略
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper(objectMapper));

        // And create the API client

        restClient.isRunning();

        client = new ElasticsearchClient(transport);
        asyncClient = new ElasticsearchAsyncClient(transport);
    }

    private List<Host> parseAddresses(String addresses) {
        List<Host> hostList = new ArrayList<>();
        String[] hosts = addresses.split(",");
        System.out.println(Arrays.toString(hosts));
        for (String host : hosts) {
            String[] splitArr = host.split(":");
            Host hostObj = new Host();
            hostObj.setHostName(splitArr[0]);
            hostObj.setPort(Integer.parseInt(splitArr[1]));
            hostList.add(hostObj);
        }
        return hostList;
    }

    public boolean clientIsNull(){
        return client == null;
    }

    public ElasticsearchClient getClient() {
        if (client == null) {
            initializeClient();
        }
        return client;
    }

    public ElasticsearchAsyncClient getAsyncClient() {
        if (asyncClient == null) {
            initializeClient();
        }
        return asyncClient;
    }

    public boolean testConnection() {
        try {
            //通过是否能够查询集群中document的数量进行连接是否成功的判断
            getClient().count();
            return true;
        } catch (Exception e) {
            logger.error("测试连接出错:{}",e.getMessage());
            return false;
        }
    }





    // Close the client and release resources
    public void close() {
        try {
            if (client != null) {
                client._transport().close();// Close transport
                client = null;
            }
            if (asyncClient != null) {
                asyncClient._transport().close();  // Close transport
                asyncClient = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}