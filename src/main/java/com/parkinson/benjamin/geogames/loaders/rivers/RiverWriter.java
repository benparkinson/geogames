package com.parkinson.benjamin.geogames.loaders.rivers;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.loaders.countries.CountryWriter;
import com.parkinson.benjamin.geogames.loaders.features.Slugs;
import com.parkinson.benjamin.geogames.model.River;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
    return Slugs.slug(river.name(), "river") + "-" + river.qid() + ".json";
  }
}
