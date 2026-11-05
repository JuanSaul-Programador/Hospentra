FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' ./mvnw && chmod +x ./mvnw
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN mkdir -p uploads
COPY --from=builder /workspace/target/reservation-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
