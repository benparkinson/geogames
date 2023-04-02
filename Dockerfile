FROM openjdk:17-jdk-slim

RUN mvn clean install

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/geogames-0.0.1-SNAPSHOT.jar"]