# Stage 1: Build the Spring Boot backend
FROM maven:3.9.6-eclipse-temurin-17 AS backend-builder
WORKDIR /app

# Copy the pom.xml and dependency definitions first to leverage Docker cache
COPY pom.xml ./
COPY mvnw ./
COPY .mvn ./.mvn/
# Fetch dependencies
RUN mvn dependency:go-offline -B

# Copy backend source
COPY src ./src/

# Package the application (skip tests as they require a running database)
RUN mvn clean package -DskipTests

# Stage 2: Run the Spring Boot application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-builder /app/target/shopkart-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9090
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
