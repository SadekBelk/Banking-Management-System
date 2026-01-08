FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy and build the api-gateway
COPY api-gateway/pom.xml ./api-gateway/pom.xml
WORKDIR /app/api-gateway
RUN mvn dependency:go-offline -B

COPY api-gateway/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre AS runner

WORKDIR /app

COPY --from=builder /app/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 4004

ENTRYPOINT ["java", "-jar", "app.jar"]
