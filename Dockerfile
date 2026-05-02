# Use official Playwright Java image which contains all necessary browser dependencies
FROM mcr.microsoft.com/playwright/java:v1.49.0-noble

# Set the working directory
WORKDIR /app

# Copy the built JAR file from the host
# We expect 'mvn clean install' to have been run on the host
COPY hud-backend/target/hud-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8888

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
