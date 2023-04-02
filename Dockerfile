FROM maven:3.8.3-openjdk-17

RUN mvn clean install

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/geogames-0.0.1-SNAPSHOT.jar"]