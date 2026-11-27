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

  private static String key(String address) {
    return ClientAddress.key(request(address), 1);
  }

  @Test
  void ipv6AddressesInOneSlash64ShareAKey() {
    assertThat(key("2001:db8:1:2::1")).isEqualTo(key("2001:db8:1:2:ffff:ffff:ffff:ffff"));
  }

  @Test
  void differentSlash64sHaveDifferentKeys() {
    assertThat(key("2001:db8:1:2::1")).isNotEqualTo(key("2001:db8:1:3::1"));
  }

  @Test
  void fullAndCompressedIpv6FormsShareAKey() {
    assertThat(key("2001:0db8:0000:0000:0000:0000:0000:0001")).isEqualTo(key("2001:db8::1"));
  }

  @Test
  void anIpv4MappedAddressIsTheIpv4Address() {
    assertThat(key("::ffff:198.51.100.1")).isEqualTo(key("198.51.100.1"));
  }

  @Test
  void ipv4IsTheWholeAddress() {
    assertThat(key("198.51.100.1")).isEqualTo("198.51.100.1").isNotEqualTo(key("198.51.100.2"));
  }

  @Test
  void textThatIsNotAnAddressIsReturnedAsItIsWithoutALookup() {
    assertThat(key("example.com")).isEqualTo("example.com");
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
