package br.imd.ufrn.gateway;

interface ProtocolHandler<T> {
    void startServer();
    void handleRequest(T socket, int nextServer);
}