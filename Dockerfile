# Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY .mvn .mvn
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q -DskipTests package

# Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 mcp
COPY --from=build /app/target/spring-ai-mcp-brasil-*.jar app.jar
USER mcp
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
