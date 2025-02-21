package com.refactor.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.expression.spel.ast.NullLiteral;

import java.io.IOException;
import java.sql.SQLOutput;
import java.util.Base64;

/**
 * @description:
 * @author: xyc
 * @date: 2024-09-27 11:01
 */
public class HarborUtils {
    private static final String HARBOR_API_BASE_URL = "http://172.16.17.46:8085/api/v2.0";

    public static boolean harborProjectExists(String harborHost, String harborProject) {
        String projectUrl = harborHost + "/projects?name=" + harborProject;

        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(projectUrl);

            // Set the appropriate headers (e.g., authorization, content-type)
            // based on your Harbor configuration
            // request.setHeader("Authorization", "Bearer your-access-token");
            // request.setHeader("Content-Type", "application/json");

            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println("statusCode" + statusCode);
            if (statusCode == 200) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonElement jsonElement = JsonParser.parseString(responseBody);
                System.out.println(jsonElement.getAsJsonArray().size());

                if (jsonElement.isJsonArray()) {
                    JsonArray projects = jsonElement.getAsJsonArray();
                    if(projects.size() == 0)
                        createHarborProject(HARBOR_API_BASE_URL, harborProject);
                    return true;
                } else if (jsonElement.isJsonObject()) {
                    System.out.println("-------TEST----------");
                    JsonObject project = jsonElement.getAsJsonObject();
                    // Handle the case where the response is a single JSON object
                    return true;
                } else {
                    return false;
                }
            }  else {
                return false;
            }
        } catch (IOException e) {
            // Handle the exception
            return false;
        }
    }

    private static void createHarborProject(String harborHost, String harborProject) {
        String projectUrl = harborHost + "/projects";
        System.out.println("---"+projectUrl);
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(projectUrl);
            request.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(("admin" + ":" + "Harbor12345").getBytes()));
            // Set the appropriate headers (e.g., authorization, content-type)
            // based on your Harbor configuration
            // request.setHeader("Authorization", "Bearer your-access-token");
            request.setHeader("Content-Type", "application/json");

            // Create the project JSON payload
            JsonObject projectPayload = new JsonObject();
            projectPayload.addProperty("project_name", harborProject);
            projectPayload.addProperty("public", true); // You can adjust the project visibility as needed

            StringEntity entity = new StringEntity(projectPayload.toString());
            request.setEntity(entity);

            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 201) {
                System.out.println("Harbor project created successfully: " + harborProject);
            } else {
                String responseBody = EntityUtils.toString(response.getEntity());
                System.err.println("Error creating Harbor project: " + responseBody);
            }
        } catch (IOException e) {
            System.err.println("Error creating Harbor project: " + e.getMessage());
        }
    }


    public static void main(String[] args) {
        System.out.println(harborProjectExists("http://172.16.17.46:8085/api/v2.0", "testreposity"));
    }
}