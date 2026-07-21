# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# This matches the jar inside your monolith/target directory
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-Xmx256m", "-Xms256m", "-jar", "app.jar"]