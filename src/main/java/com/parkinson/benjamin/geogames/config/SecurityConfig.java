package com.parkinson.benjamin.geogames.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * The request body cap on the API and the hardening headers on every response. Both run first, so
 * an oversized body is refused before anything else reads it.
 */
@Configuration
public class SecurityConfig {

  @Bean
  FilterRegistrationBean<RequestBodyLimitFilter> requestBodyLimitFilter() {
    var registration = new FilterRegistrationBean<>(new RequestBodyLimitFilter());
    registration.addUrlPatterns("/api/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }

  @Bean
  FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
    var registration = new FilterRegistrationBean<>(new SecurityHeadersFilter());
    registration.addUrlPatterns("/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
    return registration;
  }
}
