package com.parkinson.benjamin.geogames.service.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestRateLimitInterceptorTest {

  private final Clock clock = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneOffset.UTC);

  private RequestRateLimitInterceptor interceptor(int limit) {
    return new RequestRateLimitInterceptor(clock, limit, Duration.ofMinutes(1), 100, 1);
  }

  private static MockHttpServletRequest request(String method) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/countries");
    request.setRemoteAddr("198.51.100.1");
    return request;
  }

  private static boolean call(RequestRateLimitInterceptor interceptor, String method) {
    return interceptor.preHandle(request(method), new MockHttpServletResponse(), new Object());
  }

  @Test
  void isOffWhenTheLimitIsZero() {
    RequestRateLimitInterceptor interceptor = interceptor(0);

    for (int i = 0; i < 50; i++) {
      assertThat(call(interceptor, "GET")).isTrue();
    }
  }

  @Test
  void refusesTheRequestOverTheLimitWithTheWait() {
    RequestRateLimitInterceptor interceptor = interceptor(2);
    call(interceptor, "GET");
    call(interceptor, "GET");

    assertThatThrownBy(() -> call(interceptor, "GET"))
        .isInstanceOfSatisfying(
            TooManyRequestsException.class, e -> assertThat(e.retryAfterSeconds()).isEqualTo(60));
  }

  @Test
  void doesNotCountOptionsRequests() {
    RequestRateLimitInterceptor interceptor = interceptor(1);

    for (int i = 0; i < 5; i++) {
      assertThat(call(interceptor, "OPTIONS")).isTrue();
    }
    assertThat(call(interceptor, "GET")).isTrue();
  }
}
