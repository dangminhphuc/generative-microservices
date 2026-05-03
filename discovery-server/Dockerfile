FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
# Root POM + all module POMs needed so Maven can resolve the multi-module reactor.
# Only discovery-server/src is copied — no source from unrelated services.
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY account-service/pom.xml account-service/pom.xml
COPY transfer-service/pom.xml transfer-service/pom.xml
COPY transaction-history-service/pom.xml transaction-history-service/pom.xml
COPY discovery-server/pom.xml discovery-server/pom.xml
COPY discovery-server/src discovery-server/src
RUN apk add --no-cache maven && \
    mvn -pl discovery-server -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /app/discovery-server/target/*.jar app.jar
EXPOSE 8761
ENV SPRING_PROFILES_ACTIVE=docker
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8761/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
