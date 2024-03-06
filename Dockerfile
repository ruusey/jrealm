FROM maven:3.8.6-openjdk-11-slim as BUILD
WORKDIR /app
COPY . .
RUN mvn clean package

FROM openjdk:11-jre-slim-bullseye

COPY --from=BUILD /app/target/jrealm-0.3.3.jar /jrealm-0.3.3.jar
EXPOSE 2222
ENTRYPOINT [ "java", "-jar", "jrealm-0.3.3.jar", "-server", "host.docker.internal" ]