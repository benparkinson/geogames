package com.parkinson.benjamin.geogames.service.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

/** Works out the address of the client behind our proxy, without trusting what the client sends. */
public final class ClientAddress {

  private ClientAddress() {}

  /**
   * The {@code X-Forwarded-For} entry {@code hops} from the right (the proxy appends the address it
   * saw, so the leftmost entries are the client's to forge). With no header, or fewer entries than
   * hops, the connection's own address.
   */
  public static String from(HttpServletRequest request, int hops) {
    String header = request.getHeader("X-Forwarded-For");
    if (header != null && !header.isBlank() && hops > 0) {
      String[] entries = header.split(",");
      int index = entries.length - hops;
      if (index >= 0) {
        String entry = entries[index].trim();
        if (!entry.isEmpty()) {
          return entry;
        }
      }
    }
    return request.getRemoteAddr();
  }
}
