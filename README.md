# Geogames (working title)

## Build and run

### Running locally

First build the application:

```mvn clean install```

This will create a jar in the `target` directory. Run this jar as you would normally:

```java -jar target/${NAME_OF_JAR_FILE}```

This will start your server on port 8080. Go to localhost:8080 to play.

### Running in development mode

Packaging and running the jar is slow. When developing locally it's sometimes desirable to decouple
the front and backends so you can easily restart the backend after a change, or run the frontend in
hot reload mode.

This currently requires lots of manual changes, it'll be configured at some point.

1. Add `@CrossOrigin(origins = "http://localhost:3000")` to the endpoints in the server
1. Run the server via intelliJ by running the main method in `GeogamesApplication`.
1. Update the backend endpoint in your client to http://localhost:8080/
1. Then, head to the `client` dir in your terminal and run `npm start`

After these steps, you have the server running on 8080 and the frontend on 3000 pointing to your
server. Updating and saving a file in the frontend reloads the frontend in milliseconds rather than
minutes.