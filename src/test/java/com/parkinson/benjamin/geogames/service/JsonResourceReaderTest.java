package com.parkinson.benjamin.geogames.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import tools.jackson.core.type.TypeReference;

class JsonResourceReaderTest {

  record Thing(String name, int size) {}

  private ResourcePatternResolver resolver;
  private JsonResourceReader reader;

  @BeforeEach
  void setUp() {
    resolver = mock(ResourcePatternResolver.class);
    reader = new JsonResourceReader(resolver);
  }

  private static Resource json(String description, String text) {
    return new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8), description);
  }

  /** A resource that remembers whether the stream it handed out was closed. */
  private static class TrackedResource extends ByteArrayResource {
    boolean closed;

    TrackedResource(String text) {
      super(text.getBytes(StandardCharsets.UTF_8), "tracked");
    }

    @Override
    public InputStream getInputStream() {
      return new ByteArrayInputStream(getByteArray()) {
        @Override
        public void close() throws IOException {
          closed = true;
          super.close();
        }
      };
    }
  }

  @Test
  void readsAFileIntoAType() {
    when(resolver.getResource("classpath:data/thing.json"))
        .thenReturn(json("thing.json", "{\"name\":\"a\",\"size\":3}"));

    assertThat(reader.read("data/thing.json", Thing.class)).isEqualTo(new Thing("a", 3));
  }

  @Test
  void ignoresPropertiesTheTypeDoesNotHave() {
    when(resolver.getResource("classpath:data/thing.json"))
        .thenReturn(json("thing.json", "{\"name\":\"a\",\"size\":3,\"extra\":true}"));

    assertThat(reader.read("data/thing.json", Thing.class)).isEqualTo(new Thing("a", 3));
  }

  @Test
  void readsAFileIntoAGenericType() {
    when(resolver.getResource("classpath:data/things.json"))
        .thenReturn(json("things.json", "{\"x\":{\"name\":\"a\",\"size\":1}}"));

    Map<String, Thing> things = reader.read("data/things.json", new TypeReference<>() {});

    assertThat(things).containsExactly(Map.entry("x", new Thing("a", 1)));
  }

  @Test
  void readsEveryMatchingFileInOrder() throws IOException {
    when(resolver.getResources("classpath*:/data/*.json"))
        .thenReturn(
            new Resource[] {
              json("one.json", "{\"name\":\"one\",\"size\":1}"),
              json("two.json", "{\"name\":\"two\",\"size\":2}")
            });

    assertThat(reader.readAll("classpath*:/data/*.json", Thing.class))
        .containsExactly(new Thing("one", 1), new Thing("two", 2));
  }

  @Test
  void closesEveryStreamItOpens() throws IOException {
    TrackedResource first = new TrackedResource("{\"name\":\"one\",\"size\":1}");
    TrackedResource second = new TrackedResource("{\"name\":\"two\",\"size\":2}");
    when(resolver.getResources(anyString())).thenReturn(new Resource[] {first, second});

    reader.readAll("classpath*:/data/*.json", Thing.class);

    assertThat(first.closed).isTrue();
    assertThat(second.closed).isTrue();
  }

  @Test
  void aMissingFileFailsNamingIt() {
    JsonResourceReader real = new JsonResourceReader(new PathMatchingResourcePatternResolver());

    assertThatThrownBy(() -> real.read("data/does-not-exist.json", Thing.class))
        .isInstanceOf(UncheckedIOException.class)
        .hasMessageContaining("does-not-exist.json");
  }

  @Test
  void invalidJsonFailsNamingTheFile() {
    when(resolver.getResource("classpath:data/bad.json")).thenReturn(json("bad.json", "{not json"));

    assertThatThrownBy(() -> reader.read("data/bad.json", Thing.class))
        .isInstanceOf(UncheckedIOException.class)
        .hasMessageContaining("bad.json");
  }

  @Test
  void invalidJsonInOneOfManyFilesFailsNamingThatFile() throws IOException {
    when(resolver.getResources(anyString()))
        .thenReturn(
            new Resource[] {
              json("good.json", "{\"name\":\"a\",\"size\":1}"), json("broken.json", "[1,")
            });

    assertThatThrownBy(() -> reader.readAll("classpath*:/data/*.json", Thing.class))
        .isInstanceOf(UncheckedIOException.class)
        .hasMessageContaining("broken.json");
  }

  @Test
  void aPatternThatCannotBeListedFailsNamingThePattern() throws IOException {
    when(resolver.getResources("classpath*:/nope/*.json")).thenThrow(new IOException("denied"));

    assertThatThrownBy(() -> reader.readAll("classpath*:/nope/*.json", Thing.class))
        .isInstanceOf(UncheckedIOException.class)
        .hasMessageContaining("classpath*:/nope/*.json");
  }

  @Test
  void readsNothingWhenNoFileMatches() throws IOException {
    when(resolver.getResources(anyString())).thenReturn(new Resource[0]);

    List<Thing> things = new ArrayList<>(reader.readAll("classpath*:/none/*.json", Thing.class));

    assertThat(things).isEmpty();
  }
}
