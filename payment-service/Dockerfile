# Universal Dockerfile for TicketBlitz Services
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Copy the built jar file
# This assumes 'mvn clean package' has been run and a single jar exists in target/
COPY target/*.jar app.jar

# Expose a default port (this is informational, actual mapping happens in Compose)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
