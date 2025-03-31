package br.imd.ufrn.server;

public class Server {
  public static void main(String[] args) {
    switch (args[0].toUpperCase()) {
      case "TCP":
        System.out.println("TCP not implemented yet");
        break;
      case "UDP":
        System.out.println("UDP not implemented yet");
        break;
      case "HTTP":
        System.out.println("HTTP not implemented yet");
        break;
      case "GRPC":
        System.out.println("gRPC not implemented yed");
        break;
      default:
        throw new IllegalArgumentException("Protocolo não suportado");
    }
  }
}
