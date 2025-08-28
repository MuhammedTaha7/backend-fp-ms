# ================================
# Dockerfile (at project root, for troubleshooting)
# ================================
# Stage 1: Build the entire multi-module project
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean install -DskipTests
# Add this line to list the contents of the target directories
RUN ls -R /app/
# End of troubleshooting step

# Stage 2: Create runtime images for each service
# This stage is for the edusphere-service
FROM eclipse-temurin:17-jre AS edusphere-service-runtime
WORKDIR /app
COPY --from=build /app/eduSphere-service/target/edusphere-service-*.jar app.jar
RUN mkdir -p /app/uploads
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# This stage is for the community-service
FROM eclipse-temurin:17-jre AS community-service-runtime
WORKDIR /app
COPY --from=build /app/community-service/target/community-service-*.jar app.jar
RUN mkdir -p /app/uploads
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# This stage is for the extension-service
FROM eclipse-temurin:17-jre AS extension-service-runtime
WORKDIR /app
COPY --from=build /app/extension-service/target/extension-service-*.jar app.jar
RUN mkdir -p /app/uploads
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]