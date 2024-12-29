package com.parkinson.benjamin.geogames.loaders.tripoint;

import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.Tripoint;
import com.parkinson.benjamin.geogames.model.Tripoint.TripointBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class TripointComputer {

  private final Pattern decimalCoordPattern = Pattern.compile(
      "^(\\d+)°(\\d+\\.?\\d*)?′?(\\d+\\.?\\d*)?″?([NSEW])$");

  public List<Tripoint> computeTripoints(List<Country> countries) throws IOException {
    Map<String, Country> countriesByName = new HashMap<>();
    countries.forEach(country -> {
      countriesByName.put(country.name().toLowerCase(), country);
      country.additionalNames()
          .forEach(additionalName -> countriesByName.put(additionalName.toLowerCase(), country)
          );
    });

    List<Tripoint> tripoints = new ArrayList<>();

    Document doc = Jsoup.connect("http://en.wikipedia.org/wiki/List_of_tripoints").get();
    doc.select("tr")
        .forEach(row -> {
          if (!row.children().select("a").isEmpty()
              && row.children().select(".mbox-image").isEmpty()) {

            TripointBuilder builder = new TripointBuilder();

            Elements cells = row.select("td");
            cells.forEach(cell -> {
                  if (!cell.select(".flagicon").isEmpty()) {
                    cell.select("a").forEach(country -> {
                      String countryName = country.text();

                      Country lookedUpCountry = countriesByName.get(countryName.toLowerCase());
                      if (lookedUpCountry != null) {
                        builder.country(lookedUpCountry);
                      } else {
                        System.out.println("couldn't find " + countryName);
                      }
                    });
                  }
                  if (!cell.select(".geo-inline").isEmpty()) {
                    cell.select(".latitude").forEach(
                        latitude -> builder.latitude(convertToDecimalCoordinate(latitude.text())));
                    cell.select(".longitude").forEach(
                        longitude -> builder.longitude(convertToDecimalCoordinate(longitude.text())));
                  }
                }
            );

            tripoints.add(builder.build());
          }
        });

    return tripoints;
  }

  private double convertToDecimalCoordinate(String dmsString) {
    Matcher matcher = decimalCoordPattern.matcher(dmsString);

    if (!matcher.find()) {
      throw new IllegalArgumentException("Invalid DMS format: " + dmsString);
    }

    int degrees = Integer.parseInt(matcher.group(1));
    double minutes = matcher.group(2) != null ? Double.parseDouble(matcher.group(2)) : 0;
    double seconds = matcher.group(3) != null ? Double.parseDouble(matcher.group(3)) : 0;
    String direction = matcher.group(4);

    double dd = degrees + minutes / 60.0 + seconds / 3600.0;

    if (direction.equals("S") || direction.equals("W")) {
      dd = dd * -1;
    }

    return dd;
  }

}
