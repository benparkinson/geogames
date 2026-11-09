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

1. Run the server via intelliJ by running the main method in `GeogamesApplication`.
1. Then, head to the `client` dir in your terminal and run `npm run dev`

After these steps, you have the server running on 8080 and the frontend on 3000 pointing to your
server. Updating and saving a file in the frontend reloads the frontend in milliseconds rather than
minutes.


## Usage statistics

The server records one anonymous row for every round a player answers, so the owner can see which
game types and which questions (rivers, countries, lakes, tripoints) are hardest. A row holds the
UTC date, the game type, what was asked (for example `river:Q3392`), how the round ended, the hints
and wrong guesses used and the points scored. Rounds of one game share a random `play_id`, so
abandoned games can be spotted.

Never recorded: IP addresses, user agents, headers, either game id (the id in the URLs could be
matched against request logs), the time of day, or anything stored in the browser. There are no
cookies and no client-side identifier, so no consent banner is needed.

The rows go to the `stat_*` tables in the same database as the games (created by a Flyway
migration): one row per answered round in `stat_round`, one per game in `stat_game`, and the repeated
names in small lookup tables. The `round_result` view shows them in the flat shape, so production needs no extra configuration. Nothing deletes them when a game expires.
Set `GEOGAMES_STATS_ENABLED=false` on the Render web service to turn recording off.
