FROM openjdk:17-jdk-slim

CMD ["./mvnw", "clean", "install"]

CMD ["java", "-jar", "target/geogames-0.0.1-SNAPSHOT.jar"]

EXPOSE 8080