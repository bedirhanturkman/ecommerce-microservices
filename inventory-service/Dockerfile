FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY common-events/pom.xml common-events/pom.xml

RUN mvn \
    -f common-events/pom.xml \
    dependency:go-offline \
    -Dmaven.test.skip=true

COPY common-events/src common-events/src

RUN mvn \
    -f common-events/pom.xml \
    clean install \
    -Dmaven.test.skip=true

COPY inventory-service/pom.xml inventory-service/pom.xml

RUN mvn \
    -f inventory-service/pom.xml \
    dependency:go-offline \
    -Dmaven.test.skip=true

COPY inventory-service/src inventory-service/src

RUN mvn \
    -f inventory-service/pom.xml \
    clean package \
    -Dmaven.test.skip=true


FROM eclipse-temurin:25-jre-noble

WORKDIR /app

COPY --from=build \
    /workspace/inventory-service/target/*.jar \
    app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]