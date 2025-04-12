package br.imd.ufrn.gateway;

import java.net.Socket;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

interface ProtocolHandler<T> {
  void startServer();

  void handleRequest(Socket clientSocket, Supplier<Integer> getNextServer);

  void handleServerRegister(IntConsumer registerServer);
}
