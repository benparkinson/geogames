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
   * The static export writes privacy.html, which the jar would only serve at /privacy.html. This
   * forward makes /privacy load directly too (the games pages have no such forward).
   */
  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/privacy").setViewName("forward:/privacy.html");
  }
}
