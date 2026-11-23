package com.parkinson.benjamin.geogames.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Browser hardening headers on every response from the jar (the API, and the static site when the
 * jar serves it). The API only ever returns JSON, so its responses also get a CSP that allows
 * nothing: a response opened directly in a browser can't run or load anything. The static site has
 * no CSP here, because the Next export uses inline scripts.
 */
public class SecurityHeadersFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-Frame-Options", "DENY");
    response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
      response.setHeader(
          "Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'; sandbox");
    }
    chain.doFilter(request, response);
  }
}
