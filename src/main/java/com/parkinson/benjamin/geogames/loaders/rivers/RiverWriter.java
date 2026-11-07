package com.parkinson.benjamin.geogames.loaders.rivers;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.loaders.countries.CountryWriter;
import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Writes one {@link River} JSON file per river. */
public class RiverWriter {

  /** Clears {@code *.json} in the folder, then writes the rivers. Returns the files written. */
  public List<Path> write(List<RiverCandidate> rivers, Map<String, RiverFacts> facts, Path outDir)
      throws IOException {
    Files.createDirectories(outDir);
    new CountryWriter().clearOutput(outDir);

    List<Path> written = new ArrayList<>();
    for (RiverCandidate river : rivers) {
      Path file = outDir.resolve(fileName(river));
      Files.writeString(
          file,
          GeoJson.prettyWriter().writeValueAsString(toRiver(river, facts.get(river.qid()))) + "\n");
      written.add(file);
    }
    return written;
  }

  static River toRiver(RiverCandidate candidate, RiverFacts facts) {
    List<List<List<Double>>> coordinates = new ArrayList<>();
    for (List<double[]> line : candidate.lines()) {
      List<List<Double>> points = new ArrayList<>();
      for (double[] p : line) {
        points.add(List.of(p[0], p[1]));
      }
      coordinates.add(points);
    }
    GeoData geoData =
        new GeoData(
            "Feature",
            new GeoProperties(candidate.name()),
            new GeoGeometry("MultiLineString", coordinates));
    return new River(
        candidate.qid(),
        candidate.name(),
        candidate.aliases(),
        candidate.continent(),
        geoData,
        facts.source(),
        facts.mouth(),
        facts.countries());
  }

  static String fileName(RiverCandidate river) {
    return slug(river.name()) + "-" + river.qid() + ".json";
  }

  static String slug(String name) {
    String slug =
        Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");
    return slug.isEmpty() ? "river" : slug;
  }
}
