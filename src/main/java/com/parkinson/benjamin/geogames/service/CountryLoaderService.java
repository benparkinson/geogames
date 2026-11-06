package com.parkinson.benjamin.geogames.service;

import static com.parkinson.benjamin.geogames.helper.CountryHelper.convertCountryNameToFileName;

import com.parkinson.benjamin.geogames.model.Country;
import com.parkinson.benjamin.geogames.model.CountryName;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

@Service
public class CountryLoaderService extends FileLoaderService {

  private final ResourcePatternResolver resourcePatternResolver;

  @Autowired
  public CountryLoaderService(ResourcePatternResolver resourcePatternResolver) {
    super();
    this.resourcePatternResolver = resourcePatternResolver;
  }

  @Cacheable("country")
  public Country loadCountryByName(String name) throws IOException {
    String fileName = convertCountryNameToFileName(name);
    return loadFile("processed/countries/" + fileName, Country.class);
  }

  @Cacheable("countryNames")
  public List<CountryName> loadCountryNames() throws IOException {
    Resource[] resources =
        resourcePatternResolver.getResources("classpath*:/processed/countries/*.json");
    List<CountryName> names = new ArrayList<>(resources.length);

    for (Resource resource : resources) {
      try (InputStream inputStream = resource.getInputStream()) {
        CountryName country = readFile(inputStream, CountryName.class);
        List<String> additionalNames =
            country.additionalNames() == null ? List.of() : country.additionalNames();
        names.add(new CountryName(country.name(), additionalNames));
      }
    }

    names.sort(Comparator.comparing(CountryName::name));
    return List.copyOf(names);
  }
}
