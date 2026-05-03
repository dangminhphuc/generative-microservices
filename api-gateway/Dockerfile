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
COPY api-gateway/src api-gateway/src
RUN apk add --no-cache maven && \
    mvn -pl common,api-gateway -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
