package com.parkinson.benjamin.geogames.loaders.lakes;

import com.parkinson.benjamin.geogames.helper.GeoJson;
import com.parkinson.benjamin.geogames.loaders.countries.CountryWriter;
import com.parkinson.benjamin.geogames.loaders.features.Slugs;
import com.parkinson.benjamin.geogames.model.Lake;
import com.parkinson.benjamin.geogames.model.geojson.GeoData;
import com.parkinson.benjamin.geogames.model.geojson.GeoGeometry;
import com.parkinson.benjamin.geogames.model.geojson.GeoProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Writes one {@link Lake} JSON file per lake. */
public class LakeWriter {

  /** Clears {@code *.json} in the folder, then writes the lakes. Returns the files written. */
  public List<Path> write(
      List<LakeCandidate> lakes, Map<String, List<String>> countries, Path outDir)
      throws IOException {
    Files.createDirectories(outDir);
    new CountryWriter().clearOutput(outDir);

    List<Path> written = new ArrayList<>();
    for (LakeCandidate lake : lakes) {
      Path file = outDir.resolve(fileName(lake));
      Files.writeString(
          file,
          GeoJson.prettyWriter().writeValueAsString(toLake(lake, countries.get(lake.qid())))
              + "\n");
      written.add(file);
    }
    return written;
  }

  static Lake toLake(LakeCandidate candidate, List<String> countries) {
    List<List<List<List<Double>>>> polygons = new ArrayList<>();
    for (List<List<double[]>> polygon : candidate.polygons()) {
      List<List<List<Double>>> rings = new ArrayList<>();
      for (List<double[]> ring : polygon) {
        List<List<Double>> points = new ArrayList<>();
        for (double[] p : ring) {
          points.add(List.of(p[0], p[1]));
        }
        rings.add(points);
      }
      polygons.add(rings);
    }
    GeoGeometry geometry =
        polygons.size() == 1
            ? new GeoGeometry("Polygon", polygons.get(0))
            : new GeoGeometry("MultiPolygon", polygons);
    GeoData geoData = new GeoData("Feature", new GeoProperties(candidate.name()), geometry);
    return new Lake(
        candidate.qid(),
        candidate.name(),
        candidate.aliases(),
        candidate.continent(),
        geoData,
        countries);
  }

  static String fileName(LakeCandidate lake) {
    return Slugs.slug(lake.name(), "lake") + "-" + lake.qid() + ".json";
  }
}
