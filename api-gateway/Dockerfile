FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn dependency:go-offline -DskipTests

COPY src ./src

RUN mvn clean package -DskipTests


FROM eclipse-temurin:25-jre-noble

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]