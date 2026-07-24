FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY customer-service/pom.xml customer-service/pom.xml

RUN mvn \
    -f customer-service/pom.xml \
    dependency:go-offline \
    -DskipTests

COPY customer-service/src customer-service/src

RUN mvn \
    -f customer-service/pom.xml \
    clean package \
    -DskipTests


FROM eclipse-temurin:25-jre-noble

WORKDIR /app

COPY --from=build \
    /workspace/customer-service/target/*.jar \
    app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]