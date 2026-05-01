FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY transaction-history-service/pom.xml transaction-history-service/pom.xml
COPY common/src common/src
COPY transaction-history-service/src transaction-history-service/src
RUN apk add --no-cache maven && \
    mvn -pl common,transaction-history-service -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/transaction-history-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
