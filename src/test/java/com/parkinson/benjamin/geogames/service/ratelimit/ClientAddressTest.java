package com.parkinson.benjamin.geogames.service.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientAddressTest {

  private static MockHttpServletRequest request(String forwardedFor) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("10.0.0.1");
    if (forwardedFor != null) {
      request.addHeader("X-Forwarded-For", forwardedFor);
    }
    return request;
  }

  @Test
  void oneHopTakesTheLastEntry() {
    assertThat(ClientAddress.from(request("9.9.9.9"), 1)).isEqualTo("9.9.9.9");
  }

  @Test
  void aForgedLeftmostEntryIsIgnored() {
    assertThat(ClientAddress.from(request("6.6.6.6, 9.9.9.9"), 1)).isEqualTo("9.9.9.9");
  }

  @Test
  void twoHopsTakeTheSecondFromTheRight() {
    assertThat(ClientAddress.from(request("6.6.6.6, 9.9.9.9, 8.8.8.8"), 2)).isEqualTo("9.9.9.9");
  }

  @Test
  void spacesAreTrimmed() {
    assertThat(ClientAddress.from(request("  9.9.9.9  "), 1)).isEqualTo("9.9.9.9");
  }

  @Test
  void noHeaderFallsBackToTheRemoteAddress() {
    assertThat(ClientAddress.from(request(null), 1)).isEqualTo("10.0.0.1");
  }

  @Test
  void tooFewEntriesFallBackToTheRemoteAddress() {
    assertThat(ClientAddress.from(request("9.9.9.9"), 2)).isEqualTo("10.0.0.1");
  }
}
