package com.parkinson.benjamin.geogames.service;

import static com.parkinson.benjamin.geogames.helper.CountryHelper.convertCountryNameToFileName;

import com.parkinson.benjamin.geogames.model.Country;
import java.io.IOException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CountryLoaderService extends FileLoaderService {

  @Cacheable("country")
  public Country loadCountryByName(String name) throws IOException {
    String fileName = convertCountryNameToFileName(name);
    return loadFile("processed/countries/" + fileName, Country.class);
  }
}
