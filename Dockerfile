FROM maven:3.9-eclipse-temurin-26 AS build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
COPY client /usr/src/app/client
RUN mvn -f /usr/src/app/pom.xml clean install -DskipTests

FROM eclipse-temurin:17-jre
COPY --from=build /usr/src/app/target/geogames-0.0.1-SNAPSHOT.jar /usr/app/geogames-0.0.1-SNAPSHOT.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Xmx400M", "/usr/app/geogames-0.0.1-SNAPSHOT.jar"]
