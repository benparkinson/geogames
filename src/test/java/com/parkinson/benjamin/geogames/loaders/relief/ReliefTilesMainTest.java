package com.parkinson.benjamin.geogames.loaders.relief;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parkinson.benjamin.geogames.loaders.CapturedOutput;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReliefTilesMainTest {

  @TempDir Path dir;

  private Path writeTiff(int width, int height) throws IOException {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        image.setRGB(x, y, x < width / 2 ? 0x804020 : 0x2040A0);
      }
    }
    Path tiff = dir.resolve("source.tif");
    assertThat(ImageIO.write(image, "tiff", tiff.toFile())).isTrue();
    return tiff;
  }

  @Test
  void refusesAnOutputFolderNotNamedRelief() throws IOException {
    Path tiff = writeTiff(72, 36);
    Path precious = Files.createDirectories(dir.resolve("precious"));
    Files.writeString(precious.resolve("keep.txt"), "keep");

    assertThatThrownBy(() -> ReliefTilesMain.run(tiff, precious, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("relief");
    assertThat(precious.resolve("keep.txt")).exists();
  }

  @Test
  void refusesAnImageThatIsNotTwoToOne() throws IOException {
    Path tiff = writeTiff(40, 40);

    assertThatThrownBy(() -> ReliefTilesMain.run(tiff, dir.resolve("relief"), 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("2:1");
  }

  @Test
  void refusesAFileThatIsNotAnImage() throws IOException {
    Path text = dir.resolve("notes.tif");
    Files.writeString(text, "not an image");

    assertThatThrownBy(() -> ReliefTilesMain.run(text, dir.resolve("relief"), 1))
        .isInstanceOf(IOException.class);
  }

  @Test
  void replacesTheOldTilesAndSummarisesTheNewOnes() throws Exception {
    Path tiff = writeTiff(72, 36);
    Path relief = Files.createDirectories(dir.resolve("relief"));
    Files.writeString(relief.resolve("stale.txt"), "old");

    String printed =
        CapturedOutput.of(
            () -> ReliefTilesMain.main(new String[] {tiff.toString(), relief.toString(), "2"}));

    assertThat(relief.resolve("stale.txt")).doesNotExist();
    assertThat(relief.resolve("0/0/0.jpg")).isRegularFile();
    assertThat(relief.resolve("2/3/3.jpg")).isRegularFile();
    assertThat(printed).contains("Zoom 0: 1 tiles", "Zoom 1: 4 tiles", "Zoom 2: 16 tiles");
    assertThat(printed).contains("Total: 21 tiles");
  }

  @Test
  void summaryAddsUpTheTiles() {
    assertThat(new ReliefTilesMain.Summary(new int[] {1, 4, 16}, 10).totalTiles()).isEqualTo(21);
  }
}
