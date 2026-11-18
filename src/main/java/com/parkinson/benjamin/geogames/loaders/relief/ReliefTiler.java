package com.parkinson.benjamin.geogames.loaders.relief;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Cuts an equirectangular world image (longitude -180..180, latitude 90..-90) into Web Mercator map
 * tiles: 256 pixels square, {@code z/x/y.jpg}, {@code y = 0} at the top. Every zoom is rendered
 * straight from the source, averaging a grid of bilinear samples inside each output pixel so the
 * low zooms are not aliased. The output is deterministic.
 */
public final class ReliefTiler {

  static final int TILE_SIZE = 256;
  static final float JPEG_QUALITY = 0.85f;

  private ReliefTiler() {}

  /**
   * The longitude and latitude (degrees) of a position given in pixels from the top-left of the
   * whole Web Mercator world at zoom {@code z}.
   */
  static double[] tilePixelToLonLat(int z, double globalX, double globalY) {
    double n = TILE_SIZE * Math.pow(2, z);
    double lon = globalX / n * 360.0 - 180.0;
    double lat = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - 2 * globalY / n))));
    return new double[] {lon, lat};
  }

  /** The (fractional) column and row in the source whose pixel centre is at integer positions. */
  static double[] lonLatToSourcePixel(double lon, double lat, int width, int height) {
    double col = (lon + 180.0) / 360.0 * width - 0.5;
    double row = (90.0 - lat) / 180.0 * height - 0.5;
    return new double[] {col, row};
  }

  /** The source copied to one array of RGB bytes, row by row. */
  record Rgb(byte[] data, int width, int height) {
    static Rgb of(BufferedImage image) {
      int width = image.getWidth();
      int height = image.getHeight();
      byte[] data = new byte[Math.multiplyExact(Math.multiplyExact(width, height), 3)];
      int[] row = new int[width];
      for (int y = 0; y < height; y++) {
        image.getRGB(0, y, width, 1, row, 0, width);
        int o = y * width * 3;
        for (int x = 0; x < width; x++) {
          int argb = row[x];
          data[o++] = (byte) (argb >> 16);
          data[o++] = (byte) (argb >> 8);
          data[o++] = (byte) argb;
        }
      }
      return new Rgb(data, width, height);
    }
  }

  /** Writes every tile of zooms {@code 0..maxZoom} under {@code outDir}. Returns the tile count. */
  public static int writeTiles(BufferedImage source, Path outDir, int maxZoom) throws IOException {
    Rgb rgb = Rgb.of(source);
    int count = 0;
    for (int z = 0; z <= maxZoom; z++) {
      int tiles = 1 << z;
      int zoom = z;
      for (int x = 0; x < tiles; x++) {
        Files.createDirectories(outDir.resolve(Integer.toString(z)).resolve(Integer.toString(x)));
      }
      // Tiles are independent and each is written to its own file, so the order is irrelevant.
      IntStream.range(0, tiles * tiles)
          .parallel()
          .forEach(
              i -> {
                int x = i / tiles;
                int y = i % tiles;
                Path file =
                    outDir
                        .resolve(Integer.toString(zoom))
                        .resolve(Integer.toString(x))
                        .resolve(y + ".jpg");
                try {
                  writeJpeg(renderTile(rgb, zoom, x, y), file);
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });
      count += tiles * tiles;
    }
    return count;
  }

  /** Renders one tile as a 256 x 256 image. */
  static BufferedImage renderTile(Rgb source, int z, int tileX, int tileY) {
    int k = Math.max(1, (int) Math.ceil(source.width() / (double) (TILE_SIZE << z)));
    BufferedImage tile = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
    double weight = 1.0 / (k * k);
    for (int py = 0; py < TILE_SIZE; py++) {
      for (int px = 0; px < TILE_SIZE; px++) {
        double r = 0;
        double g = 0;
        double b = 0;
        for (int sy = 0; sy < k; sy++) {
          double gy = tileY * TILE_SIZE + py + (sy + 0.5) / k;
          for (int sx = 0; sx < k; sx++) {
            double gx = tileX * TILE_SIZE + px + (sx + 0.5) / k;
            double[] lonLat = tilePixelToLonLat(z, gx, gy);
            double[] pos =
                lonLatToSourcePixel(lonLat[0], lonLat[1], source.width(), source.height());
            double[] rgb = bilinear(source, pos[0], pos[1]);
            r += rgb[0];
            g += rgb[1];
            b += rgb[2];
          }
        }
        int ir = clamp(r * weight);
        int ig = clamp(g * weight);
        int ib = clamp(b * weight);
        tile.setRGB(px, py, (ir << 16) | (ig << 8) | ib);
      }
    }
    return tile;
  }

  private static int clamp(double v) {
    return (int) Math.min(255, Math.max(0, Math.round(v)));
  }

  /** Bilinear sample, wrapping in longitude (columns) and clamping in latitude (rows). */
  static double[] bilinear(Rgb s, double col, double row) {
    double c0f = Math.floor(col);
    double r0f = Math.floor(row);
    double fx = col - c0f;
    double fy = row - r0f;
    int c0 = Math.floorMod((int) c0f, s.width());
    int c1 = (c0 + 1) % s.width();
    int r0 = Math.min(s.height() - 1, Math.max(0, (int) r0f));
    int r1 = Math.min(s.height() - 1, Math.max(0, (int) r0f + 1));
    double[] out = new double[3];
    for (int ch = 0; ch < 3; ch++) {
      double top = lerp(value(s, c0, r0, ch), value(s, c1, r0, ch), fx);
      double bottom = lerp(value(s, c0, r1, ch), value(s, c1, r1, ch), fx);
      out[ch] = lerp(top, bottom, fy);
    }
    return out;
  }

  private static double lerp(double a, double b, double t) {
    return a + (b - a) * t;
  }

  private static int value(Rgb s, int col, int row, int channel) {
    return s.data()[(row * s.width() + col) * 3 + channel] & 0xFF;
  }

  private static void writeJpeg(BufferedImage image, Path file) throws IOException {
    ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
    ImageWriteParam param = writer.getDefaultWriteParam();
    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
    param.setCompressionQuality(JPEG_QUALITY);
    try (OutputStream out = Files.newOutputStream(file);
        ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
      writer.setOutput(ios);
      writer.write(null, new IIOImage(image, null, null), param);
    } finally {
      writer.dispose();
    }
  }
}
