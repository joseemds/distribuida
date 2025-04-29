package br.imd.ufrn.gateway;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

interface ProtocolHandler<T> {
  void startServer();

  void handleRequest(T clientSocket, Supplier<Integer> getNextServer);

  void handleServerRegister(IntConsumer registerServer);

  void sendError(T clientSocket, String errorMessage);

  boolean isServerHealthy(Integer serverPort);
}
