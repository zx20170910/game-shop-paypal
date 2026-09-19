FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 gameshop
COPY --from=build /workspace/target/game-shop-*.jar /app/game-shop.jar
USER gameshop
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/game-shop.jar"]
