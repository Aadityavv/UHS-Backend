# ---------- Stage 1: Build ----------
    FROM eclipse-temurin:17-jdk-jammy AS builder

    # Set working directory
    WORKDIR /opt/app
    
    # Install dos2unix (if needed)
    RUN apt-get update && apt-get install -y dos2unix
    
    # Copy Maven wrapper and pom.xml first to leverage Docker cache
    COPY .mvn/ .mvn
    COPY mvnw pom.xml ./
    
    # Make mvnw executable and fix line endings
    RUN chmod +x mvnw
    RUN dos2unix mvnw 
    
    # Download all dependencies for offline build
    RUN ./mvnw dependency:go-offline
    
    # Copy the source code
    COPY src ./src
    
    # Package the application (skipping tests)
    RUN ./mvnw clean package -DskipTests
    
    # ---------- Stage 2: Runtime ----------
    FROM eclipse-temurin:17-jre-jammy
    
    # Set working directory
    WORKDIR /opt/app
    
    # Expose port 8081 (as per your application)
    EXPOSE 8081
    
    # Copy the JAR from builder stage
    COPY --from=builder /opt/app/target/*.jar app.jar
    
    # Run the Spring Boot application
    ENTRYPOINT ["java", "-jar", "app.jar"]
    