FROM openjdk:17-jdk-slim-buster

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar

ENTRYPOINT ["java", "-jar", "/application.jar"]