FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre
COPY --from=build /app/target/jrealm.jar /jrealm.jar
EXPOSE 2222
ENV DATA_SERVER_ADDR=host.docker.internal
ENTRYPOINT ["sh", "-c", "java -jar /jrealm.jar -server $DATA_SERVER_ADDR"]
