FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# First, copy and build the banking-proto module (shared dependency)
COPY banking-proto/pom.xml ./banking-proto/pom.xml
COPY banking-proto/src ./banking-proto/src
RUN mvn -f banking-proto/pom.xml clean install -DskipTests

# Now copy and build the transaction-service
COPY transaction-service/pom.xml ./transaction-service/pom.xml
WORKDIR /app/transaction-service
RUN mvn dependency:go-offline -B

COPY transaction-service/src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre AS runner

WORKDIR /app

COPY --from=builder /app/transaction-service/target/transaction-service-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 4002

ENTRYPOINT ["java", "-jar", "app.jar"]