# Phase 1: build source code using maven
FROM maven:3.9.12-amazoncorretto-17-debian AS build
WORKDIR /app
COPY pom.xml .
COPY sun_checks-9.3.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Phase 2: Create image for running app
FROM eclipse-temurin:17-jre-alpine-3.23
WORKDIR /app
# Create user to run app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]
