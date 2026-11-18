package com.parkinson.benjamin.geogames.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/** Adds an ETag (a hash of the body) to the place name lists, so browsers can revalidate them. */
@Configuration
public class PlaceNameEtagConfig {

  @Bean
  FilterRegistrationBean<ShallowEtagHeaderFilter> placeNameEtagFilter() {
    var registration = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
    registration.addUrlPatterns("/api/countries", "/api/rivers", "/api/lakes");
    return registration;
  }
}
