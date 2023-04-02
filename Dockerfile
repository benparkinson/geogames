FROM maven:3.8.3-openjdk-17 as build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
COPY client /usr/src/app/client
RUN mvn -f /usr/src/app/pom.xml clean install

FROM openjdk:17.0.1-jdk-slim
COPY --from=build /usr/src/app/target/geogames-0.0.1-SNAPSHOT.jar /usr/app/geogames-0.0.1-SNAPSHOT.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/usr/app/geogames-0.0.1-SNAPSHOT.jar"]