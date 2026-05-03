FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY discovery-server/pom.xml discovery-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY account-service/pom.xml account-service/pom.xml
COPY transfer-service/pom.xml transfer-service/pom.xml
COPY transaction-history-service/pom.xml transaction-history-service/pom.xml
COPY common/src common/src
COPY transfer-service/src transfer-service/src
RUN apk add --no-cache maven && \
    mvn -pl common,transfer-service -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/transfer-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
