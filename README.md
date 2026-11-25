# Geoggernaut

The code, the Maven artifact and the repository are still called `geogames`.

## Build and run

### Prerequisites

- A Java 25 JDK, with `JAVA_HOME` pointing at it.
- Git.

That's all. Maven comes with the repo (`./mvnw`, or `mvnw.cmd` in PowerShell or cmd), and the
first build downloads Node 24 into `target/node`. No database is needed: locally the games are
kept in an in-memory H2 database, which is emptied when the server stops.

### Running locally

Build the application, with the frontend pointed at your local server:

```bash
NEXT_PUBLIC_API_URL=http://localhost:8080 ./mvnw clean install -DskipTests
```

In PowerShell: `$env:NEXT_PUBLIC_API_URL = "http://localhost:8080"; .\mvnw.cmd clean install -DskipTests`.
Leave out `-DskipTests` to run all the tests as well (much slower).

Without `NEXT_PUBLIC_API_URL`, the bundled site calls the production backend on Render instead of
your local one.

This creates a jar in the `target` directory. Run it:

```bash
java -jar target/geogames-0.0.1-SNAPSHOT.jar
```

This starts the server on port 8080. Go to http://localhost:8080 to play.

### Running in development mode

Packaging and running the jar is slow. When developing locally it's sometimes desirable to decouple
the front and backends so you can easily restart the backend after a change, or run the frontend in
hot reload mode.

1. Start the backend: run the main method in `GeogamesApplication` from your IDE, or
   `./mvnw spring-boot:run -Dskip.frontend` (`-Dskip.frontend` stops Maven building the frontend
   first).
1. In another terminal, go to the `client` dir and run `npm ci` (first time only, or after
   `package-lock.json` changes), then `npm run dev`.

Use Node 24 (`client/.nvmrc`): an older system Node may be too old for Next 16. If yours is older,
use the one Maven installed by putting `target/node` first on your `PATH` (run a Maven build once
to create it).

After these steps, you have the server running on 8080 and the frontend on http://localhost:3000
pointing to your server. Updating and saving a file in the frontend reloads the frontend in
milliseconds rather than minutes.

### Checking your changes

The `scripts/check-*.sh` scripts run the linters and tests and print only the failures, for
example `bash scripts/check-java.sh` and `bash scripts/check-frontend.sh`. See `CLAUDE.md` for the
full list.


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

## Licence

The code is source-available under the [PolyForm Noncommercial License 1.0.0](LICENSE): you may read, run, change and share it for non-commercial purposes. See [NOTICE.md](NOTICE.md) for the required notice and for the data and images that are not covered by this licence.
