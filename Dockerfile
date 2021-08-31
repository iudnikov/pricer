FROM openjdk:8-jdk-alpine

COPY ./target/*.jar /app/
WORKDIR /app

ENTRYPOINT java -jar pricer.jar
