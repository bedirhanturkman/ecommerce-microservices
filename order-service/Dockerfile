FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

# Önce common-events POM'u kopyalanır.
COPY common-events/pom.xml common-events/pom.xml

# Common-events bağımlılıkları indirilir.
RUN mvn \
    -f common-events/pom.xml \
    dependency:go-offline \
    -Dmaven.test.skip=true

# Common-events kaynak kodu kopyalanır ve local Maven repository'ye kurulur.
COPY common-events/src common-events/src

RUN mvn \
    -f common-events/pom.xml \
    clean install \
    -Dmaven.test.skip=true

# Order Service POM'u ve bağımlılıkları hazırlanır.
COPY order-service/pom.xml order-service/pom.xml

RUN mvn \
    -f order-service/pom.xml \
    dependency:go-offline \
    -Dmaven.test.skip=true

# Order Service kaynak kodu kopyalanır ve build edilir.
COPY order-service/src order-service/src

RUN mvn \
    -f order-service/pom.xml \
    clean package \
    -Dmaven.test.skip=true


FROM eclipse-temurin:25-jre-noble

WORKDIR /app

COPY --from=build \
    /workspace/order-service/target/*.jar \
    app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]