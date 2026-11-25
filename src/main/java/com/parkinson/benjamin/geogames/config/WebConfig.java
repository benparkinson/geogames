package com.parkinson.benjamin.geogames.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final String[] allowedOrigins;

  public WebConfig(@Value("${geogames.cors.allowed-origins}") String[] allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins(allowedOrigins)
        .allowedMethods("GET", "POST", "HEAD");
  }

  /**
   * The static export writes privacy.html, daily.html and challenge.html, which the jar would only
   * serve at /privacy.html and so on. These forwards make /privacy, /daily and /challenge load
   * directly too (the games pages have no such forward). /daily and /challenge are share links.
   */
  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/privacy").setViewName("forward:/privacy.html");
    registry.addViewController("/daily").setViewName("forward:/daily.html");
    registry.addViewController("/challenge").setViewName("forward:/challenge.html");
  }
}
