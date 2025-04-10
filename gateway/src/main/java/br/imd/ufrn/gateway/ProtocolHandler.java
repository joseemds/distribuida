package br.imd.ufrn.gateway;


import java.net.Socket;
import java.util.function.Consumer;
import java.util.function.Supplier;

interface ProtocolHandler<T> {
    void startServer();
    void handleRequest(Socket clientSocket, Supplier<Integer> getNextServer);
    void handleServerRegister();
}