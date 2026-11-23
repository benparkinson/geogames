package com.parkinson.benjamin.geogames.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.parkinson.benjamin.geogames.controllers.HealthController;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void everyResponseHasTheHardeningHeaders() throws Exception {
    mockMvc
        .perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
        .andExpect(header().doesNotExist("Content-Security-Policy"));
  }

  @Test
  void apiResponsesAllowNothingToRun() throws Exception {
    mockMvc
        .perform(get("/api/countries"))
        .andExpect(
            header()
                .string(
                    "Content-Security-Policy",
                    "default-src 'none'; frame-ancestors 'none'; sandbox"));
  }

  @Test
  void anApiBodyOverTheCapIsRefusedBeforeItIsRead() throws Exception {
    mockMvc
        .perform(
            post("/api/games")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new byte[RequestBodyLimitFilter.MAX_BODY_BYTES + 1]))
        .andExpect(status().isContentTooLarge());
  }

  @Test
  void aBodyAtTheCapIsReadInFull() throws Exception {
    var request = new MockHttpServletRequest("POST", "/api/games");
    request.setContent(new byte[RequestBodyLimitFilter.MAX_BODY_BYTES]);
    var chain = new ReadingChain();

    new RequestBodyLimitFilter().doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(chain.bytesRead).isEqualTo(RequestBodyLimitFilter.MAX_BODY_BYTES);
  }

  @Test
  void aBodyWithNoLengthStopsAtTheCap() {
    var request =
        new MockHttpServletRequest("POST", "/api/games") {
          @Override
          public long getContentLengthLong() {
            return -1; // chunked: the length isn't declared
          }
        };
    request.setContent(new byte[RequestBodyLimitFilter.MAX_BODY_BYTES + 1]);

    assertThatThrownBy(
            () ->
                new RequestBodyLimitFilter()
                    .doFilter(request, new MockHttpServletResponse(), new ReadingChain()))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("over");
  }

  @Test
  void readingOneByteAtATimeAlsoStopsAtTheCap() throws Exception {
    var request = new MockHttpServletRequest("POST", "/api/games");
    request.setContent(new byte[RequestBodyLimitFilter.MAX_BODY_BYTES + 1]);
    var chain =
        new MockFilterChain() {
          @Override
          public void doFilter(
              jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
              throws IOException {
            var in = req.getInputStream();
            assertThat(req.getInputStream()).isSameAs(in);
            for (int i = 0; i < RequestBodyLimitFilter.MAX_BODY_BYTES; i++) {
              assertThat(in.read()).isZero();
            }
            assertThat(in.isFinished()).isFalse();
            assertThat(in.isReady()).isTrue();
            assertThatThrownBy(in::read).isInstanceOf(IOException.class);
          }
        };
    // A declared length over the cap would be refused before reading, so hide it.
    var undeclared =
        new jakarta.servlet.http.HttpServletRequestWrapper(request) {
          @Override
          public long getContentLengthLong() {
            return -1;
          }
        };

    new RequestBodyLimitFilter().doFilter(undeclared, new MockHttpServletResponse(), chain);
  }

  @Test
  void theEndOfTheBodyAndAsyncListenersPassThrough() throws Exception {
    var delegate = mock(jakarta.servlet.ServletInputStream.class);
    when(delegate.read()).thenReturn(-1);
    var listener = mock(jakarta.servlet.ReadListener.class);
    var request =
        new MockHttpServletRequest("POST", "/api/games") {
          @Override
          public jakarta.servlet.ServletInputStream getInputStream() {
            return delegate;
          }
        };
    var chain =
        new MockFilterChain() {
          @Override
          public void doFilter(
              jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
              throws IOException {
            var in = req.getInputStream();
            assertThat(in.read()).isEqualTo(-1);
            in.setReadListener(listener);
          }
        };

    new RequestBodyLimitFilter().doFilter(request, new MockHttpServletResponse(), chain);

    verify(delegate).setReadListener(listener);
  }

  /** Reads the whole body, a byte array at a time, as Jackson does. */
  private static final class ReadingChain extends MockFilterChain {
    long bytesRead;

    @Override
    public void doFilter(
        jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
        throws IOException {
      var in = request.getInputStream();
      byte[] buffer = new byte[4096];
      int n;
      while ((n = in.read(buffer)) != -1) {
        bytesRead += n;
      }
    }
  }
}
