package com.parkinson.benjamin.geogames.service.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.HexFormat;

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

  /**
   * The key a client is limited by: {@link #from} with an IPv6 address cut to its /64 prefix (one
   * network is given a whole /64, so each address in it would otherwise get a fresh limit). IPv4,
   * including an IPv4-mapped IPv6 address, is the whole address. Text that isn't an address is
   * returned as it is. It is parsed as a literal only, so there is never a DNS lookup: the header
   * is client text and could be a host name.
   */
  public static String key(HttpServletRequest request, int hops) {
    String address = from(request, hops);
    try {
      InetAddress parsed = InetAddress.ofLiteral(address);
      if (parsed instanceof Inet6Address v6) {
        return HexFormat.of().formatHex(v6.getAddress(), 0, 8) + "/64";
      }
      return parsed.getHostAddress();
    } catch (IllegalArgumentException e) {
      return address;
    }
  }
}
