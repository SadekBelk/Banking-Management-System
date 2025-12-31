FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# First, copy and build the banking-proto module (shared dependency)
COPY banking-proto/pom.xml ./banking-proto/pom.xml
COPY banking-proto/src ./banking-proto/src
RUN mvn -f banking-proto/pom.xml clean install -DskipTests

# Now copy and build the payment-service
COPY payment-service/pom.xml ./payment-service/pom.xml
WORKDIR /app/payment-service
RUN mvn dependency:go-offline -B

COPY payment-service/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre AS runner

WORKDIR /app

COPY --from=builder /app/payment-service/target/payment-service-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 4003

ENTRYPOINT ["java", "-jar", "app.jar"]
