FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY product-service/pom.xml product-service/pom.xml

RUN mvn \
    -f product-service/pom.xml \
    dependency:go-offline \
    -DskipTests

COPY product-service/src product-service/src

RUN mvn \
    -f product-service/pom.xml \
    clean package \
    -DskipTests


FROM eclipse-temurin:25-jre-noble

WORKDIR /app

COPY --from=build \
    /workspace/product-service/target/*.jar \
    app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]