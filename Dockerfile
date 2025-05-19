# Use official OpenJDK 21 runtime as base image
FROM openjdk:21-jdk-slim

# Create a non-root user
RUN addgroup --system spring && adduser --system --ingroup spring spring

# Use non-root user
USER spring:spring

# Define JAR location as build argument
ARG JAR_FILE=target/*.jar

# Copy the JAR file to the container
COPY ${JAR_FILE} app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "/app.jar"]
