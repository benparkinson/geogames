package com.parkinson.benjamin.geogames.loaders.relief;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReliefTilerTest {

  private static final int WEST = 0xC03020;
  private static final int EAST = 0x2060D0;

  @TempDir Path out;

  @Test
  void tilePixelToLonLatMapsTheMercatorWorld() {
    assertThat(ReliefTiler.tilePixelToLonLat(0, 128, 128)).containsExactly(0.0, 0.0);
    double[] corner = ReliefTiler.tilePixelToLonLat(0, 0, 0);
    assertThat(corner[0]).isEqualTo(-180.0);
    assertThat(corner[1]).isCloseTo(85.0511, within(0.0001));
    assertThat(ReliefTiler.tilePixelToLonLat(1, 256, 256)).containsExactly(0.0, 0.0);
  }

  @Test
  void lonLatToSourcePixelPlacesCornersAndCentre() {
    assertThat(ReliefTiler.lonLatToSourcePixel(-180, 90, 360, 180)).containsExactly(-0.5, -0.5);
    assertThat(ReliefTiler.lonLatToSourcePixel(180, -90, 360, 180)).containsExactly(359.5, 179.5);
    assertThat(ReliefTiler.lonLatToSourcePixel(0, 0, 360, 180)).containsExactly(179.5, 89.5);
  }

  static BufferedImage twoColours() {
    BufferedImage image = new BufferedImage(360, 180, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < 180; y++) {
      for (int x = 0; x < 360; x++) {
        image.setRGB(x, y, x < 180 ? WEST : EAST);
      }
    }
    return image;
  }

  private static void assertColour(BufferedImage tile, int expected) {
    int rgb = tile.getRGB(128, 128);
    for (int shift : new int[] {16, 8, 0}) {
      assertThat((rgb >> shift) & 0xFF).isCloseTo((expected >> shift) & 0xFF, within(10));
    }
  }

  @Test
  void cutsTheWesternAndEasternHalvesIntoTheirTiles() throws IOException {
    int count = ReliefTiler.writeTiles(twoColours(), out, 1);

    assertThat(count).isEqualTo(5);
    for (String tile : new String[] {"0/0/0", "1/0/0", "1/0/1", "1/1/0", "1/1/1"}) {
      assertThat(out.resolve(tile + ".jpg")).isRegularFile();
    }
    assertColour(ImageIO.read(out.resolve("1/0/0.jpg").toFile()), WEST);
    assertColour(ImageIO.read(out.resolve("1/0/1.jpg").toFile()), WEST);
    assertColour(ImageIO.read(out.resolve("1/1/0.jpg").toFile()), EAST);
    assertColour(ImageIO.read(out.resolve("1/1/1.jpg").toFile()), EAST);
    BufferedImage world = ImageIO.read(out.resolve("0/0/0.jpg").toFile());
    assertThat(world.getWidth()).isEqualTo(256);
    assertThat(world.getHeight()).isEqualTo(256);
  }

  @Test
  void cuttingTwiceGivesIdenticalBytes() throws IOException {
    Path first = out.resolve("a");
    Path second = out.resolve("b");
    ReliefTiler.writeTiles(twoColours(), first, 1);
    ReliefTiler.writeTiles(twoColours(), second, 1);

    for (String tile : new String[] {"0/0/0", "1/0/0", "1/0/1", "1/1/0", "1/1/1"}) {
      assertThat(Files.readAllBytes(second.resolve(tile + ".jpg")))
          .isEqualTo(Files.readAllBytes(first.resolve(tile + ".jpg")));
    }
  }

  @Test
  void samplingWrapsInLongitudeAndClampsInLatitude() {
    ReliefTiler.Rgb rgb = ReliefTiler.Rgb.of(twoColours());

    // Between the last column (east) and the first (west): a blend across the antimeridian.
    double[] wrapped = ReliefTiler.bilinear(rgb, 359.5, 10);
    assertThat(wrapped[0]).isCloseTo((0xC0 + 0x20) / 2.0, within(0.01));
    // Above the top row and below the bottom row: the edge row.
    double[] above = ReliefTiler.bilinear(rgb, 10, -5);
    assertThat(above[0]).isEqualTo(0xC0);
    double[] below = ReliefTiler.bilinear(rgb, 10, 500);
    assertThat(below[2]).isEqualTo(0x20);
  }
}
