FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY auth-service/pom.xml auth-service/pom.xml

RUN mvn \
    -f auth-service/pom.xml \
    dependency:go-offline \
    -DskipTests

COPY auth-service/src auth-service/src

RUN mvn \
    -f auth-service/pom.xml \
    clean package \
    -DskipTests


FROM eclipse-temurin:25-jre-noble

WORKDIR /app

COPY --from=build \
    /workspace/auth-service/target/*.jar \
    app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]