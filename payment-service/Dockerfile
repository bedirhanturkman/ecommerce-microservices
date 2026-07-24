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

COPY payment-service/pom.xml payment-service/pom.xml

RUN mvn \
    -f payment-service/pom.xml \
    dependency:go-offline \
    -Dmaven.test.skip=true

COPY payment-service/src payment-service/src

RUN mvn \
    -f payment-service/pom.xml \
    clean package \
    -Dmaven.test.skip=true


FROM eclipse-temurin:25-jre-noble

WORKDIR /app

COPY --from=build \
    /workspace/payment-service/target/*.jar \
    app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]