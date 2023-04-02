FROM openjdk:17-jdk-slim

RUN ./mvnw clean install

CMD ["java", "-jar", "target/geogames-0.0.1-SNAPSHOT.jar"]

EXPOSE 8080