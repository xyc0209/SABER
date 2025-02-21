package com.refactor.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @description: Tool class related to docker
 * @author: xyc
 * @date: 2024-09-27 10:11
 */
public class DockerUtils {

    public static void generateDockerfile(String jarFilePath, String jarFileName, int exposedPort) {
        File dockerfileFile = new File(jarFilePath +"\\Dockerfile");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dockerfileFile))) {
            writer.write("# Use Java 8 as the base image");
            writer.newLine();
            writer.write("FROM openjdk:8");
            writer.newLine();
            writer.newLine();
            writer.write("# Set the working directory");
            writer.newLine();
            writer.write("WORKDIR /app");
            writer.newLine();
            writer.newLine();
            writer.write("# Copy the JAR file to the container");
            writer.newLine();
            writer.write("COPY " + jarFileName + " /app/" +  jarFileName);
            writer.newLine();
            writer.newLine();
            writer.write("# Expose the application port");
            writer.newLine();
            writer.write("EXPOSE " + exposedPort);
            writer.newLine();
            writer.newLine();
            writer.write("# Set the startup command");
            writer.newLine();
            writer.write("ENTRYPOINT [\"java\", \"-jar\", \"" + jarFileName + "\"]");
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error generating Dockerfile: " + e.getMessage());
        }
    }


}