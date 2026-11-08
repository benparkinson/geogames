package com.parkinson.benjamin.geogames.loaders;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** Runs code and returns what it printed to {@code System.out}, one line per element. */
public final class CapturedOutput {

  /** Code that may throw. */
  public interface Action {
    void run() throws Exception;
  }

  private CapturedOutput() {}

  public static String of(Action action) throws Exception {
    PrintStream original = System.out;
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
    try {
      action.run();
    } finally {
      System.setOut(original);
    }
    return bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
  }
}
