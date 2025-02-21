FROM openjdk:8

# Set the working directory
WORKDIR /app

# Copy the JAR file to the container
COPY ./target/clusterAgent-0.0.1-SNAPSHOT.jar clusterAgent-0.0.1-SNAPSHOT.jar

# Expose the application port
EXPOSE 8086

# Set the startup command
ENTRYPOINT ["java", "-jar", "clusterAgent-0.0.1-SNAPSHOT.jar"]