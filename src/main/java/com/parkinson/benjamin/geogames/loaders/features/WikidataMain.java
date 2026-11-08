package com.parkinson.benjamin.geogames.loaders.features;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/**
 * Usage: WikidataMain &lt;naturalEarth.geojson&gt; &lt;outFile wikidata.json&gt;
 *
 * <p>Serves any Natural Earth file with {@code wikidataid} properties (rivers, lakes). The only
 * feature program that uses the network. Run it once and commit the output; everything else reads
 * {@code wikidata.json}.
 */
public class WikidataMain {

  private static final URI ENDPOINT = URI.create("https://query.wikidata.org/sparql");
  private static final String USER_AGENT =
      "geogames-data-loader/0.1 (https://github.com/benparkinson/geogames)";
  private static final int BATCH_SIZE = 200;
  private static final int MAX_RETRIES = 3;
  private static final Pattern QID = Pattern.compile("Q\\d+");

  public static void main(String[] args) throws IOException, InterruptedException {
    TreeSet<String> qids = new TreeSet<>();
    for (NaturalEarthFeature feature :
        new NaturalEarthReader().read(Path.of(args[0]), Set.of()).values()) {
      if (QID.matcher(feature.qid()).matches()) {
        qids.add(feature.qid());
      }
    }
    System.out.println("Looking up " + qids.size() + " Wikidata items");

    HttpClient client = HttpClient.newHttpClient();
    Map<String, WikidataEntry> result = new TreeMap<>();
    List<String> all = new ArrayList<>(qids);
    for (int i = 0; i < all.size(); i += BATCH_SIZE) {
      List<String> batch = all.subList(i, Math.min(all.size(), i + BATCH_SIZE));
      if (i > 0) {
        Thread.sleep(1000);
      }
      result.putAll(parse(post(client, buildQuery(batch))));
      System.out.println("Fetched " + result.size() + " / " + all.size());
    }

    Path out = Path.of(args[1]);
    Files.writeString(out, GeoJson.prettyWriter().writeValueAsString(result) + "\n");
  }

  static String buildQuery(List<String> qids) {
    StringBuilder values = new StringBuilder();
    for (String qid : qids) {
      if (!QID.matcher(qid).matches()) {
        throw new IllegalArgumentException("Not a QID: " + qid);
      }
      values.append(" wd:").append(qid);
    }
    return """
    SELECT ?item ?label ?sitelinks
           (GROUP_CONCAT(DISTINCT ?alt; separator="|") AS ?aliases)
           (GROUP_CONCAT(DISTINCT ?contLabel; separator="|") AS ?continents)
           (GROUP_CONCAT(DISTINCT ?countryContLabel; separator="|") AS ?countryContinents)
    WHERE {
      VALUES ?item {%s }
      OPTIONAL { ?item rdfs:label ?label FILTER(LANG(?label) = "en") }
      OPTIONAL { ?item wikibase:sitelinks ?sitelinks }
      OPTIONAL { ?item skos:altLabel ?alt FILTER(LANG(?alt) = "en") }
      OPTIONAL { ?item wdt:P30 ?cont . ?cont rdfs:label ?contLabel FILTER(LANG(?contLabel) = "en") }
      OPTIONAL { ?item wdt:P17/wdt:P30 ?cc . ?cc rdfs:label ?countryContLabel FILTER(LANG(?countryContLabel) = "en") }
    }
    GROUP BY ?item ?label ?sitelinks
    """
        .formatted(values);
  }

  static Map<String, WikidataEntry> parse(String json) {
    Map<String, WikidataEntry> result = new TreeMap<>();
    JsonNode bindings = GeoJson.lenientReader().readTree(json).path("results").path("bindings");
    for (JsonNode row : bindings) {
      String item = value(row, "item");
      String qid = item.substring(item.lastIndexOf('/') + 1);
      String sitelinks = value(row, "sitelinks");
      result.put(
          qid,
          new WikidataEntry(
              value(row, "label"),
              sitelinks.isEmpty() ? 0 : Integer.parseInt(sitelinks),
              split(value(row, "aliases")),
              split(value(row, "continents")),
              split(value(row, "countryContinents"))));
    }
    return result;
  }

  private static String value(JsonNode row, String variable) {
    JsonNode node = row.path(variable).path("value");
    return node.isMissingNode() || node.isNull() ? "" : node.asString();
  }

  private static List<String> split(String joined) {
    return joined.isEmpty() ? List.of() : List.of(joined.split("\\|"));
  }

  private static String post(HttpClient client, String query)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(ENDPOINT)
            .header("Accept", "application/sparql-results+json")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("User-Agent", USER_AGENT)
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    "query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)))
            .build();

    for (int attempt = 0; ; attempt++) {
      HttpResponse<String> response =
          client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      int status = response.statusCode();
      if (status == 200) {
        return response.body();
      }
      boolean retryable = status == 429 || status >= 500;
      if (!retryable || attempt >= MAX_RETRIES) {
        throw new IOException("Wikidata returned " + status + ": " + response.body());
      }
      long waitMs = 2000L * (1L << attempt);
      System.out.println("Got " + status + ", retrying in " + waitMs + " ms");
      Thread.sleep(waitMs);
    }
  }
}
