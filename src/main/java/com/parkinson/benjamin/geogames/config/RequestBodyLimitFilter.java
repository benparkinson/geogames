package com.parkinson.benjamin.geogames.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Caps the size of a request body. Every API request is a few hundred bytes of JSON, but neither
 * Tomcat nor Jackson limits a JSON body, and the JVM has 400 MB: a few large bodies at once could
 * run it out of memory. A declared length over the cap is a 413 before anything is read; a body
 * without a length (chunked) fails as soon as it passes the cap, which Spring reports as a 400.
 */
public class RequestBodyLimitFilter extends OncePerRequestFilter {

  /** Far above the largest real request (an answer or a guess is under 200 bytes). */
  public static final int MAX_BODY_BYTES = 16 * 1024;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getContentLengthLong() > MAX_BODY_BYTES) {
      response.sendError(HttpStatus.CONTENT_TOO_LARGE.value(), "Request body too large");
      return;
    }
    chain.doFilter(new LimitedRequest(request), response);
  }

  private static final class LimitedRequest extends HttpServletRequestWrapper {

    private ServletInputStream stream;

    LimitedRequest(HttpServletRequest request) {
      super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      if (stream == null) {
        stream = new LimitedInputStream(super.getInputStream());
      }
      return stream;
    }
  }

  private static final class LimitedInputStream extends ServletInputStream {

    private final ServletInputStream delegate;
    private long read;

    LimitedInputStream(ServletInputStream delegate) {
      this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
      int b = delegate.read();
      if (b != -1) {
        count(1);
      }
      return b;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      int n = delegate.read(buffer, offset, length);
      if (n > 0) {
        count(n);
      }
      return n;
    }

    private void count(int n) throws IOException {
      read += n;
      if (read > MAX_BODY_BYTES) {
        throw new IOException("Request body is over " + MAX_BODY_BYTES + " bytes");
      }
    }

    @Override
    public boolean isFinished() {
      return delegate.isFinished();
    }

    @Override
    public boolean isReady() {
      return delegate.isReady();
    }

    @Override
    public void setReadListener(ReadListener listener) {
      delegate.setReadListener(listener);
    }
  }
}
