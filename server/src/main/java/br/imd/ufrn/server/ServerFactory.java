package br.imd.ufrn.server;

public class ServerFactory {
  public static Server getServer(String protocol) {
    return switch (protocol) {
      case "tcp" -> new TCPServer();
      case "udp" -> new UDPServer();
      case "http" -> new HttpServer();
      default -> throw new IllegalArgumentException("Protocolo não suportado");
    };
  }
}
