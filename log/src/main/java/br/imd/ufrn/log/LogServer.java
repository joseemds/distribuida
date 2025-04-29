package br.imd.ufrn.log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.imd.ufrn.log.protocol.AbstractServer;
import br.imd.ufrn.log.protocol.HttpServer;
import br.imd.ufrn.log.protocol.TCPServer;
import br.imd.ufrn.log.protocol.UDPServer;

public class LogServer {
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
  private int port;
  private AbstractServer handler;

  public LogServer(int port, String protocol) {
    this.port = port;
    this.handler =
        switch (protocol) {
          case "TCP" -> new TCPServer();
          case "UDP" -> new UDPServer();
          case "HTTP" -> new HttpServer();
          default -> throw new IllegalArgumentException("Invalid protocol");
        };
  }

  public void start() {
    this.handler.run(port, executor);
  }
}
