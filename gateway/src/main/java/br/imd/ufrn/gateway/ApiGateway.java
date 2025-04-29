package br.imd.ufrn.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ApiGateway {
  private final int port;
  private int currentServerIndex = 0;
  private final ProtocolHandler protocolHandler;
  private final List<Integer> registeredServers = new ArrayList<>();
  private final List<Integer> healthyServers = new ArrayList<>();
  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

  public ApiGateway(int port, String protocolType) {
    this.port = port;
    this.protocolHandler = createProtocolHandler(protocolType);
  }

  public void start() {
    executor.submit(() -> protocolHandler.handleServerRegister(this::registerServer));
    executor.submit(this::monitorHealth);
    this.protocolHandler.startServer();
  }

  private void registerServer(Integer port) {
    this.registeredServers.add(port);
  }

  private int getNextServer() {
    synchronized (healthyServers) {
      if (healthyServers.isEmpty()) {
        System.out.println("No healthy servers found");
        throw new IllegalStateException("No healthy servers available");
      }

      if (currentServerIndex >= healthyServers.size()) {
        currentServerIndex = 0;
      }

      int serverPort = healthyServers.get(currentServerIndex);
      currentServerIndex = (currentServerIndex + 1) % healthyServers.size();
      return serverPort;
    }
  }

  private ProtocolHandler createProtocolHandler(String protocol) {
    return switch (protocol) {
      case "TCP" -> new TcpProtocol(this.port, this.executor, this::getNextServer);
      case "UDP" -> new UdpProtocol(this.port, this.executor, this::getNextServer);
      case "HTTP" -> new HttpProtocol(this.port, this.executor, this::getNextServer);
      default -> throw new Error("Unsupported protocol");
    };
  }

  private void monitorHealth() {
    while (true) {
      List<Integer> currentHealthyServers = new ArrayList<>();

      List<Integer> serversToCheck;
      synchronized (registeredServers) {
        serversToCheck = new ArrayList<>(registeredServers);
      }

      for (Integer serverPort : serversToCheck) {
        if (protocolHandler.isServerHealthy(serverPort)) {
          currentHealthyServers.add(serverPort);
        }
      }

      synchronized (healthyServers) {
        healthyServers.clear();
        healthyServers.addAll(currentHealthyServers);
      }

      try {
        TimeUnit.SECONDS.sleep(5);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }
}
