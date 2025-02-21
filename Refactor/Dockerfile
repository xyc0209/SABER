# Use Java 8 as the base image
FROM openjdk:8

# Set the working directory
WORKDIR /app

# Copy the JAR file to the container
COPY D:/code/Service-Demo/testService1/target testService-1.0-SNAPSHOT-exec.jar

# Expose the application port
EXPOSE 8089

# Set the startup command
ENTRYPOINT ["java", "-jar", "testService-1.0-SNAPSHOT-exec.jar"]
