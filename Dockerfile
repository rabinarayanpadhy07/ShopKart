# Stage 1: Build the React frontend
FROM node:20-alpine AS frontend-builder
WORKDIR /app/ShopKart-UI
COPY ShopKart-UI/package*.json ./
RUN npm install
COPY ShopKart-UI/ ./
RUN npm run build

# Stage 2: Build the Spring Boot backend
FROM maven:3.9.6-eclipse-temurin-17 AS backend-builder
WORKDIR /app
# Copy the pom.xml and dependency definitions first to leverage Docker cache
COPY ShopKart/pom.xml ./ShopKart/
COPY ShopKart/mvnw ./ShopKart/
COPY ShopKart/.mvn ./ShopKart/.mvn/
WORKDIR /app/ShopKart
# Fetch dependencies
RUN mvn dependency:go-offline -B

# Copy backend source
WORKDIR /app
COPY ShopKart/src ./ShopKart/src/

# Copy the built React assets from Stage 1 into the Spring Boot static resources folder
COPY --from=frontend-builder /app/ShopKart-UI/dist/ /app/ShopKart/src/main/resources/static/

# Package the application
WORKDIR /app/ShopKart
RUN mvn clean package -DskipTests

# Stage 3: Run the Spring Boot application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-builder /app/ShopKart/target/shopkart-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 9090
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
