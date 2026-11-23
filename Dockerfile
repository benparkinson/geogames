FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /usr/src/app
# Maven Central answers 429 to busy shared addresses (CI runners): retry for longer than the default 3.
ENV MAVEN_ARGS="-B -Daether.connector.http.retryHandler.count=10"
# The dependencies get a layer of their own, reused until pom.xml changes, so most builds download
# nothing from Maven Central. go-offline misses a few plugins that load lazily; the build fetches those.
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src src
COPY client client
# Lint has its own CI job; here it would only turn the docker check red for the same reason.
RUN mvn clean install -DskipTests -Dskip.lint

FROM eclipse-temurin:25-jre
# Run as an unprivileged user: the app writes nothing outside /tmp.
RUN useradd --system --no-create-home --shell /usr/sbin/nologin geogames
USER geogames
COPY --from=build /usr/src/app/target/geogames-0.0.1-SNAPSHOT.jar /usr/app/geogames-0.0.1-SNAPSHOT.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Xmx400M", "/usr/app/geogames-0.0.1-SNAPSHOT.jar"]
