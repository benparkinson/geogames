package com.parkinson.benjamin.geogames.loaders.relief;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/**
 * Usage: ReliefTilesMain &lt;source.tif&gt; &lt;outDir&gt; &lt;maxZoom&gt;
 *
 * <p>Cuts the Natural Earth II raster (plain longitude/latitude, 2:1) into JPEG map tiles {@code
 * outDir/z/x/y.jpg} for zooms 0..maxZoom. {@code outDir} is deleted first, so it must be a folder
 * named {@code relief}. Offline and deterministic. Run it with {@code -Xmx4g}.
 */
public class ReliefTilesMain {

  /**
   * What a run wrote.
   *
   * @param tilesPerZoom the number of tiles of each zoom, from zoom 0
   * @param totalBytes the size of all the tiles
   */
  public record Summary(int[] tilesPerZoom, long totalBytes) {
    public int totalTiles() {
      int total = 0;
      for (int n : tilesPerZoom) {
        total += n;
      }
      return total;
    }
  }

  public static void main(String[] args) throws IOException {
    Summary summary = run(Path.of(args[0]), Path.of(args[1]), Integer.parseInt(args[2]));
    printSummary(summary);
  }

  /** Does everything {@code main} does except print. */
  static Summary run(Path source, Path outDir, int maxZoom) throws IOException {
    Path name = outDir.toAbsolutePath().normalize().getFileName();
    if (name == null || !name.toString().equals("relief")) {
      throw new IllegalArgumentException(
          "Refusing to delete " + outDir + ": the output folder must be named 'relief'");
    }
    ImageIO.setUseCache(false);
    BufferedImage image = ImageIO.read(source.toFile());
    if (image == null) {
      throw new IOException("Not an image ImageIO can read: " + source);
    }
    if (image.getWidth() != 2 * image.getHeight()) {
      throw new IllegalArgumentException(
          "Expected a 2:1 image, got " + image.getWidth() + " x " + image.getHeight());
    }
    deleteTree(outDir);
    Files.createDirectories(outDir);
    ReliefTiler.writeTiles(image, outDir, maxZoom);

    int[] perZoom = new int[maxZoom + 1];
    for (int z = 0; z <= maxZoom; z++) {
      perZoom[z] = 1 << (2 * z);
    }
    long bytes;
    try (Stream<Path> files = Files.walk(outDir)) {
      bytes =
          files
              .filter(Files::isRegularFile)
              .mapToLong(
                  f -> {
                    try {
                      return Files.size(f);
                    } catch (IOException e) {
                      throw new java.io.UncheckedIOException(e);
                    }
                  })
              .sum();
    }
    return new Summary(perZoom, bytes);
  }

  private static void deleteTree(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(dir)) {
      for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.delete(p);
      }
    }
  }

  private static void printSummary(Summary summary) {
    for (int z = 0; z < summary.tilesPerZoom().length; z++) {
      System.out.println("Zoom " + z + ": " + summary.tilesPerZoom()[z] + " tiles");
    }
    System.out.println(
        String.format(
            Locale.ROOT,
            "Total: %d tiles, %.1f MB",
            summary.totalTiles(),
            summary.totalBytes() / 1_000_000.0));
  }
}
